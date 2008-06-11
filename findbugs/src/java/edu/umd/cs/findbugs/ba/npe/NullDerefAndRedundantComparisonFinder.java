/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * A user-friendly front end for finding null pointer dereferences
 * and redundant null comparisions.
 * 
 * @see IsNullValueAnalysis
 * @author David Hovemeyer
 */
public class NullDerefAndRedundantComparisonFinder {
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug");
	private static final boolean PRUNE_GUARANTEED_DEREFERENCES = SystemProperties.getBoolean("fnd.prune", true);

	private static final boolean DEBUG_DEREFS = SystemProperties.getBoolean("fnd.derefs.debug");

	private ClassContext classContext;
	private Method method;
	private NullDerefAndRedundantComparisonCollector collector;
	private final boolean findGuaranteedDerefs;

	private List<RedundantBranch> redundantBranchList;
	private BitSet definitelySameBranchSet;
	private BitSet definitelyDifferentBranchSet;
	private BitSet undeterminedBranchSet;
	private BitSet lineMentionedMultipleTimes;

	private IsNullValueDataflow invDataflow;
	private ValueNumberDataflow vnaDataflow;
	private UnconditionalValueDerefDataflow uvdDataflow;
	private AssertionMethods assertionMethods;

	static {
		if (DEBUG) System.out.println("fnd.debug enabled");
	}
	/**
	 * Constructor.
	 * 
	 * @param classContext the ClassContext
	 * @param method       the method to analyze
	 * @param collector    the NullDerefAndRedundantComparisonCollector used to report
	 *                     null derefs and redundant null comparisons
	 */
	public NullDerefAndRedundantComparisonFinder(
			ClassContext classContext,
			Method method,
			NullDerefAndRedundantComparisonCollector collector) {

		this.classContext = classContext;
		this.method = method;
		this.collector = collector;
		this.findGuaranteedDerefs = classContext.getAnalysisContext().getBoolProperty(
				AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS);
		this.lineMentionedMultipleTimes = ClassContext.linesMentionedMultipleTimes(method);

		this.redundantBranchList = new LinkedList<RedundantBranch>();
		this.definitelySameBranchSet = new BitSet();
		this.definitelyDifferentBranchSet = new BitSet();
		this.undeterminedBranchSet = new BitSet();
		this.assertionMethods = classContext.getAssertionMethods();
	}

	public void execute() {
		Profiler profiler = Profiler.getInstance();
		profiler.start(this.getClass());
		try {
		// Do the null-value analysis
		this.invDataflow = classContext.getIsNullValueDataflow(method);
		this.vnaDataflow = classContext.getValueNumberDataflow(method);
		if (findGuaranteedDerefs) {
			if (DEBUG_DEREFS) {
				System.out.println(
						"Checking for guaranteed derefs in " +
						method.getName());
			}
			this.uvdDataflow = classContext.getUnconditionalValueDerefDataflow(method);
		}

		// Check method and report potential null derefs and
		// redundant null comparisons.
		examineBasicBlocks();
		if (findGuaranteedDerefs) {
			examineNullValues();
		}
		examineRedundantBranches();
		}  catch (MissingClassException e) {
			AnalysisContext.reportMissingClass(e.getClassNotFoundException());
		}  catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Error while for guaranteed derefs in " +
					method.getName(), e);
		} finally {
			profiler.end(this.getClass());
		}

	}

	/**
	 * Examine basic blocks for null checks and potentially-redundant
	 * null comparisons.
	 * 
	 * @throws DataflowAnalysisException
	 * @throws CFGBuilderException
	 */
	private void examineBasicBlocks() throws DataflowAnalysisException, CFGBuilderException {
		// Look for null check blocks where the reference being checked
		// is definitely null, or null on some path
		Iterator<BasicBlock> bbIter = invDataflow.getCFG().blockIterator();
		while (bbIter.hasNext()) {
			BasicBlock basicBlock = bbIter.next();

			if (basicBlock.isNullCheck()) {
				analyzeNullCheck(classContext, method, invDataflow, basicBlock);
			} else if (!basicBlock.isEmpty()) {
				// Look for all reference comparisons where
				//    - both values compared are definitely null, or
				//    - one value is definitely null and one is definitely not null
				// These cases are not null dereferences,
				// but they are quite likely to indicate an error, so while we've got
				// information about null values, we may as well report them.
				InstructionHandle lastHandle = basicBlock.getLastInstruction();
				Instruction last = lastHandle.getInstruction();
				switch (last.getOpcode()) {
				case Constants.IF_ACMPEQ:
				case Constants.IF_ACMPNE:
					analyzeRefComparisonBranch(basicBlock, lastHandle);
					break;
				case Constants.IFNULL:
				case Constants.IFNONNULL:
					analyzeIfNullBranch(basicBlock, lastHandle);
					break;
				}
			}
		}
	}

	/**
	 * Examine null values.
	 * Report any that are guaranteed to be dereferenced on
	 * non-implicit-exception paths.
	 * 
	 * @throws CFGBuilderException 
	 * @throws DataflowAnalysisException 
	 */
	private void examineNullValues() throws CFGBuilderException, DataflowAnalysisException {
		Set<LocationWhereValueBecomesNull> locationWhereValueBecomesNullSet =
			invDataflow.getAnalysis().getLocationWhereValueBecomesNullSet();
		if (DEBUG_DEREFS) {
			System.out.println("----------------------- examineNullValues " + locationWhereValueBecomesNullSet.size());
		}

		Map<ValueNumber, SortedSet<Location>> bugStatementLocationMap =
			new HashMap<ValueNumber, SortedSet<Location>>();
		// Inspect the method for locations where a null value is guaranteed to
		// be dereferenced.  Add the dereference locations
		Map<ValueNumber, NullValueUnconditionalDeref> nullValueGuaranteedDerefMap =
			new HashMap<ValueNumber, NullValueUnconditionalDeref>();

	   // Check every location
		for (Iterator<Location> i = classContext.getCFG(method).locationIterator(); i.hasNext();) {
			Location location = i.next();

			if (DEBUG_DEREFS) {
				System.out.println("At location " + location);
			}
			if (false) {
				Instruction in = location.getHandle().getInstruction();

			   if (in instanceof InvokeInstruction && in.produceStack(classContext.getConstantPoolGen()) == 1 || in instanceof GETFIELD || in instanceof GETSTATIC) {
					IsNullValueFrame invFrame = invDataflow.getFactAfterLocation(location);
					if (invFrame.getStackDepth() > 0) {
					IsNullValue isNullValue = invFrame.getTopValue();
					if (isNullValue.isNullOnSomePath()) {
						// OK, must be from return value
						ValueNumber vn = vnaDataflow.getFactAfterLocation(location).getTopValue();
						UnconditionalValueDerefSet uvd = uvdDataflow.getFactAfterLocation(location);
						if (uvd.isUnconditionallyDereferenced(vn)) {
							// System.out.println("Found it");
							SortedSet<Location> knownNullAndDoomedAt = bugStatementLocationMap.get(vn);
							noteUnconditionallyDereferencedNullValue(  location,
									bugStatementLocationMap,
									nullValueGuaranteedDerefMap,
									uvd, isNullValue, vn);
						}
					}
					}
				}


				if (assertionMethods.isAssertionHandle (location.getHandle(), classContext.getConstantPoolGen()) ) {
					if (DEBUG_DEREFS) 
						System.out.println("Skipping because it is an assertion method ");
					continue;
				}

			}

			checkForUnconditionallyDereferencedNullValues(
					location,
					bugStatementLocationMap,
					nullValueGuaranteedDerefMap,
					vnaDataflow.getFactAtLocation(location), invDataflow.getFactAtLocation(location), uvdDataflow.getFactAfterLocation(location));
		}
		HashSet<ValueNumber> npeIfStatementCovered = new HashSet<ValueNumber>(nullValueGuaranteedDerefMap.keySet());
		Map<ValueNumber, SortedSet<Location>> bugEdgeLocationMap =
			new HashMap<ValueNumber, SortedSet<Location>>();

		// Check every non-exception control edge
		for (Iterator<Edge> i = classContext.getCFG(method).edgeIterator(); i.hasNext();) {
			Edge edge = i.next();

			if (edge.isExceptionEdge()) {
				continue;
			}

			if (DEBUG_DEREFS) {
				System.out.println("On edge " + edge.formatAsString(false));
			}

			ValueNumberFrame vnaFact = vnaDataflow.getResultFact(edge.getSource());
			ValueNumberFrame vnaEdgeFact = vnaDataflow.getFactOnEdge(edge);
			ValueNumberFrame vnaTargetFact = vnaDataflow.getStartFact(edge.getTarget());

			IsNullValueFrame invFact = invDataflow.getFactAtMidEdge(edge);

				IsNullValueFrame invSourceFact = invDataflow.getResultFact(edge.getSource());
				IsNullValueFrame invTargetFact = invDataflow.getStartFact(edge.getTarget());
				UnconditionalValueDerefSet uvdSourceFact = uvdDataflow.getStartFact(edge.getSource());
				UnconditionalValueDerefSet uvdTargetFact = uvdDataflow.getResultFact(edge.getTarget());
				Location location = Location.getLastLocation(edge.getSource());

			UnconditionalValueDerefSet uvdFact = uvdDataflow.getFactOnEdge(edge);
			// UnconditionalValueDerefSet uvdFact = uvdDataflow.getStartFact(edge.getTarget());


			if (uvdFact.isEmpty()) continue;
			if (location != null) {


				Instruction in = location.getHandle().getInstruction();
				if (assertionMethods.isAssertionInstruction(in, classContext.getConstantPoolGen())) {
					if (DEBUG_DEREFS) 
						System.out.println("Skipping because it is an assertion method ");
					continue;
				}


			checkForUnconditionallyDereferencedNullValues(
					location,
					bugEdgeLocationMap,
					nullValueGuaranteedDerefMap,
					vnaFact, invFact, uvdFact);
			}
		}
		Map<ValueNumber, SortedSet<Location>> bugLocationMap = bugEdgeLocationMap;
		bugLocationMap.putAll(bugStatementLocationMap);
		//	For each value number that is null somewhere in the
		// method, collect the set of locations where it becomes null.
		// FIXME: we may see some locations that are not guaranteed to be dereferenced (how to fix this?)
		Map<ValueNumber, Set<Location>> nullValueAssignmentMap =
			new HashMap<ValueNumber, Set<Location>>();
		for (LocationWhereValueBecomesNull lwvbn : locationWhereValueBecomesNullSet) {
			if (DEBUG_DEREFS) System.out.println("OOO " + lwvbn);
			Set<Location> locationSet = nullValueAssignmentMap.get(lwvbn.getValueNumber());
			if (locationSet == null) {
				locationSet = new HashSet<Location>();
				nullValueAssignmentMap.put(lwvbn.getValueNumber(), locationSet);
			}
			locationSet.add(lwvbn.getLocation());
			if (DEBUG_DEREFS)
				System.out.println(lwvbn.getValueNumber() + " becomes null at " + lwvbn.getLocation());
		}

		// Report 
		for (Map.Entry<ValueNumber, NullValueUnconditionalDeref> e  : nullValueGuaranteedDerefMap.entrySet()) {
			ValueNumber valueNumber = e.getKey();
			Set<Location> derefLocationSet = e.getValue().getDerefLocationSet();
			Set<Location> assignedNullLocationSet = nullValueAssignmentMap.get(valueNumber);
			if (assignedNullLocationSet == null) {
				if (DEBUG_DEREFS) {
					String where = classContext.getJavaClass().getClassName() + "." + method.getName() + ":" + method.getSignature();
					System.out.println("Problem at " + where);
					for (Location loc : derefLocationSet) {
						System.out.println("Dereference at " + loc);
					}
				}
				// TODO: figure out why this is failing
				if (false) 
				assert false: "No assigned NullLocationSet for " + valueNumber + " in " + nullValueAssignmentMap.keySet()
				+ " while analyzing " + classContext.getJavaClass().getClassName() + "." + method.getName();
				assignedNullLocationSet = Collections.<Location>emptySet();
			}
			SortedSet<Location> knownNullAndDoomedAt = bugLocationMap.get(valueNumber);

			BugAnnotation variableAnnotation = null;
			try {
				for (Location loc : derefLocationSet)  {
					variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, loc, valueNumber, vnaDataflow.getFactAtLocation(loc));
					if (variableAnnotation != null) break;
				}
				if (variableAnnotation == null) for (Location loc : knownNullAndDoomedAt) {
					variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, loc, valueNumber, vnaDataflow.getFactAtLocation(loc));
					if (variableAnnotation != null) break;
				}
				if (variableAnnotation == null) for (Location loc : assignedNullLocationSet) {
					variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, loc, valueNumber, vnaDataflow.getFactAtLocation(loc));
					if (variableAnnotation != null) break;
				}


			} catch (DataflowAnalysisException e2) {
			}
			if (variableAnnotation instanceof LocalVariableAnnotation && ((LocalVariableAnnotation)variableAnnotation).getName().equals("?") 
					&& method.getCode().getLocalVariableTable() != null)
				variableAnnotation = null;



			if (PRUNE_GUARANTEED_DEREFERENCES) {
				PostDominatorsAnalysis postDomAnalysis =
					classContext.getNonExceptionPostDominatorsAnalysis(method);
			removeStrictlyPostDominatedLocations(derefLocationSet, postDomAnalysis);

			removeStrictlyPostDominatedLocations(knownNullAndDoomedAt, postDomAnalysis);

			removeStrictlyPostDominatedLocations(assignedNullLocationSet, postDomAnalysis);
			}


			collector.foundGuaranteedNullDeref(
					assignedNullLocationSet,
					derefLocationSet,
					knownNullAndDoomedAt,
					vnaDataflow, valueNumber, 
					variableAnnotation, e.getValue(),  npeIfStatementCovered.contains(valueNumber));
		}
	}

	private void removeStrictlyPostDominatedLocations(Set<Location> locations, PostDominatorsAnalysis postDomAnalysis) {
		BitSet strictlyDominated = new BitSet();
		for(Location loc : locations) {
			BitSet allDominatedBy = postDomAnalysis.getAllDominatedBy(loc.getBasicBlock());
			allDominatedBy.clear(loc.getBasicBlock().getLabel());
			strictlyDominated.or(allDominatedBy);
		}
		LinkedList<Location> locations2 = new LinkedList<Location>(locations);

		for(Iterator<Location> i = locations.iterator(); i.hasNext(); ) {
			Location loc = i.next();
			if (strictlyDominated.get(loc.getBasicBlock().getLabel())) { 
				i.remove();
				continue;
			}
			for(Location loc2 : locations2) {
				if (loc.getBasicBlock().equals(loc2.getBasicBlock()) && loc.getHandle().getPosition() > loc2.getHandle().getPosition()) {
					i.remove();
					break;
				}
			}
		}
	}

	private static final boolean MY_DEBUG = false;
	/**
	 * Check for unconditionally dereferenced null values
	 * at a particular location in the CFG.
	 * @param thisLocation TODO
	 * @param knownNullAndDoomedAt TODO
	 * @param nullValueGuaranteedDerefMap map to be populated with null values and where they are derefed 
	 * @param vnaFrame                    value number frame to check
	 * @param invFrame                    null-value frame to check
	 * @param derefSet                    set of unconditionally derefed values at this location 
	 */
	private void checkForUnconditionallyDereferencedNullValues(
			Location thisLocation,
			Map<ValueNumber, SortedSet<Location>> knownNullAndDoomedAt,
			Map<ValueNumber, NullValueUnconditionalDeref> nullValueGuaranteedDerefMap,
			ValueNumberFrame vnaFrame, IsNullValueFrame invFrame, UnconditionalValueDerefSet derefSet) {

		if (DEBUG_DEREFS) {
			System.out.println("vna *** " + vnaFrame);
			System.out.println("inv *** " + invFrame);
			System.out.println("deref * " + derefSet);
		}

		// Make sure the frames contain meaningful information
		if (!vnaFrame.isValid() || !invFrame.isValid() || vnaFrame.getNumSlots() != invFrame.getNumSlots())  {
			return;
		}
		if (derefSet.isEmpty()) return;
		// See if there are any definitely-null values in the frame
		for (int j = 0; j < invFrame.getNumSlots(); j++) {
			IsNullValue isNullValue = invFrame.getValue(j); 
			ValueNumber valueNumber = vnaFrame.getValue(j);
			if ((isNullValue.isDefinitelyNull() || isNullValue.isNullOnSomePath() && isNullValue.isReturnValue()) && (derefSet.isUnconditionallyDereferenced(valueNumber) 
				  )) {
				if (MY_DEBUG) {
					System.out.println("Found NP bug");
					System.out.println("Location: " + thisLocation);
					System.out.println("Value number: " + valueNumber);
					System.out.println("IsNullValue frame: " + invFrame);
					System.out.println("IsNullValue value: " + isNullValue);
					System.out.println("Unconditional dere framef: " + derefSet);
					System.out.println("Unconditionally dereferenced: " + derefSet.isUnconditionallyDereferenced(valueNumber) );

				}
				noteUnconditionallyDereferencedNullValue(
						thisLocation,
						knownNullAndDoomedAt,
						nullValueGuaranteedDerefMap,
						derefSet, isNullValue, valueNumber);
			}
		}

		// See if there are any known-null values in the heap that
		// will be dereferenced in the future.
		for (Map.Entry<ValueNumber, IsNullValue> entry : invFrame.getKnownValueMapEntrySet()) {
			ValueNumber valueNumber = entry.getKey();
			IsNullValue isNullValue = entry.getValue();
			if ((isNullValue.isDefinitelyNull() || isNullValue.isNullOnSomePath() && (isNullValue.isReturnValue() || isNullValue.isFieldValue()) ) && derefSet.isUnconditionallyDereferenced(valueNumber) ) {
				
			   noteUnconditionallyDereferencedNullValue(
						thisLocation,
						knownNullAndDoomedAt,
						nullValueGuaranteedDerefMap,
						derefSet, isNullValue, valueNumber);
			}
		}
	}

	/**
	 * Note the locations where a known-null value is unconditionally
	 * dereferenced.
	 * @param thisLocation TODO
	 * @param bugLocations TODO
	 * @param nullValueGuaranteedDerefMap map of null values to sets of Locations where they are derefed
	 * @param derefSet                    set of values known to be unconditionally dereferenced
	 * @param isNullValue                 the null value
	 * @param valueNumber                 the value number of the null value
	 */
	private void noteUnconditionallyDereferencedNullValue(
			Location thisLocation, 
			Map<ValueNumber, SortedSet<Location>> bugLocations, 
			Map<ValueNumber, NullValueUnconditionalDeref> nullValueGuaranteedDerefMap, 
			UnconditionalValueDerefSet derefSet, 
			IsNullValue isNullValue, 
			ValueNumber valueNumber) {
		if (DEBUG) {
			System.out.println("%%% HIT for value number " + valueNumber + " @ " + thisLocation);
		}

		// OK, we have a null value that is unconditionally
		// derferenced.  Make a note of the locations where it
		// will be dereferenced.
		NullValueUnconditionalDeref thisNullValueDeref = nullValueGuaranteedDerefMap.get(valueNumber);
		if (thisNullValueDeref == null) {
			thisNullValueDeref = new NullValueUnconditionalDeref();
			nullValueGuaranteedDerefMap.put(valueNumber, thisNullValueDeref);
		}
		thisNullValueDeref.add(isNullValue, derefSet.getUnconditionalDerefLocationSet(valueNumber));

		if (thisLocation != null) {
			SortedSet<Location> locationsForThisBug = bugLocations.get(valueNumber);

			if (locationsForThisBug == null) {
				locationsForThisBug = new TreeSet<Location>();
				bugLocations.put(valueNumber, locationsForThisBug);
			}
			locationsForThisBug.add(thisLocation);
		}
	}

	/**
	 * Examine redundant branches.
	 */
	private void examineRedundantBranches() {
		for (RedundantBranch redundantBranch : redundantBranchList) {
			if (DEBUG) System.out.println("Redundant branch: " + redundantBranch);
			int lineNumber = redundantBranch.lineNumber;

			// The source to bytecode compiler may sometimes duplicate blocks of
			// code along different control paths.  So, to report the bug,
			// we check to ensure that the branch is REALLY determined each
			// place it is duplicated, and that it is determined in the same way.

			boolean confused = undeterminedBranchSet.get(lineNumber) ||
					(definitelySameBranchSet.get(lineNumber) && definitelyDifferentBranchSet.get(lineNumber));

			// confused if there is JSR confusion or multiple null checks with different results on the same line

			boolean reportIt = true;
		
			if (lineMentionedMultipleTimes.get(lineNumber) && confused)
				reportIt = false;
			else if (redundantBranch.location.getBasicBlock().isInJSRSubroutine() /* occurs in a JSR */
					&& confused)
				reportIt = false;
			else {
				int pc = redundantBranch.location.getHandle().getPosition();
				for(CodeException e : method.getCode().getExceptionTable()) {
					if (e.getCatchType() == 0 && e.getStartPC() != e.getHandlerPC() && e.getEndPC() <= pc && pc <= e.getEndPC()+5 )
						reportIt = false;
				}
			}
			
			if (reportIt) {
				collector.foundRedundantNullCheck(redundantBranch.location, redundantBranch);
			}
		}
	}

	private void analyzeRefComparisonBranch(
			BasicBlock basicBlock,
			InstructionHandle lastHandle) throws DataflowAnalysisException {

		Location location = new Location(lastHandle, basicBlock);

		IsNullValueFrame frame = invDataflow.getFactAtLocation(location);
		if (!frame.isValid()) {
			// Probably dead code due to pruning infeasible exception edges.
			return;
		}
		if (frame.getStackDepth() < 2)
			throw new DataflowAnalysisException("Stack underflow at " + lastHandle);

		// Find the line number.
		int lineNumber = getLineNumber(method, lastHandle);
		if (lineNumber < 0)
			return;

		int numSlots = frame.getNumSlots();
		IsNullValue top = frame.getValue(numSlots - 1);
		IsNullValue topNext = frame.getValue(numSlots - 2);

		boolean definitelySame = top.isDefinitelyNull() && topNext.isDefinitelyNull();
		boolean definitelyDifferent =
				(top.isDefinitelyNull() && topNext.isDefinitelyNotNull()) ||
				(top.isDefinitelyNotNull() && topNext.isDefinitelyNull());

		if (definitelySame || definitelyDifferent) {
			if (definitelySame) {
				if (DEBUG) System.out.println("Line " + lineNumber + " always same");
				definitelySameBranchSet.set(lineNumber);
			}
			if (definitelyDifferent) {
				if (DEBUG) System.out.println("Line " + lineNumber + " always different");
				definitelyDifferentBranchSet.set(lineNumber);
			}

			RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, top, topNext);

			// Figure out which control edge is made infeasible by the redundant comparison
			boolean wantSame = (lastHandle.getInstruction().getOpcode() == Constants.IF_ACMPEQ);
			int infeasibleEdgeType = (wantSame == definitelySame)
					? EdgeTypes.FALL_THROUGH_EDGE : EdgeTypes.IFCMP_EDGE;
			Edge infeasibleEdge = invDataflow.getCFG().getOutgoingEdgeWithType(basicBlock, infeasibleEdgeType);
			redundantBranch.setInfeasibleEdge(infeasibleEdge);

			if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
			redundantBranchList.add(redundantBranch);
		} else {
			if (DEBUG) System.out.println("Line " + lineNumber + " undetermined");
			undeterminedBranchSet.set(lineNumber);
		}
	}

	// This is called for both IFNULL and IFNONNULL instructions.
	private void analyzeIfNullBranch(
			BasicBlock basicBlock,
			InstructionHandle lastHandle) throws DataflowAnalysisException {

		Location location = new Location(lastHandle, basicBlock);

		IsNullValueFrame frame = invDataflow.getFactAtLocation(location);

		if (!frame.isValid()) {
			// This is probably dead code due to an infeasible exception edge.
			return;
		}

		IsNullValue top = frame.getTopValue();

		// Find the line number.
		int lineNumber = getLineNumber(method, lastHandle);
		if (lineNumber < 0)
			return;

		if (!(top.isDefinitelyNull() || top.isDefinitelyNotNull())) {
			if (DEBUG) System.out.println("Line " + lineNumber + " undetermined");
			undeterminedBranchSet.set(lineNumber);
			return;
		}

		// Figure out if the branch is always taken
		// or always not taken.
		short opcode = lastHandle.getInstruction().getOpcode();
		boolean definitelySame = top.isDefinitelyNull();
		if (opcode != Constants.IFNULL) definitelySame = !definitelySame;

		if (definitelySame) {
			if (DEBUG) System.out.println("Line " + lineNumber + " always same");
			definitelySameBranchSet.set(lineNumber);
		} else {
			if (DEBUG) System.out.println("Line " + lineNumber + " always different");
			definitelyDifferentBranchSet.set(lineNumber);
		}

		RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, top);

		// Determine which control edge is made infeasible by the redundant comparison
		boolean wantNull = (opcode == Constants.IFNULL);
		int infeasibleEdgeType = (wantNull == top.isDefinitelyNull())
				? EdgeTypes.FALL_THROUGH_EDGE : EdgeTypes.IFCMP_EDGE;
		Edge infeasibleEdge = invDataflow.getCFG().getOutgoingEdgeWithType(basicBlock, infeasibleEdgeType);
		redundantBranch.setInfeasibleEdge(infeasibleEdge);

		if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
		redundantBranchList.add(redundantBranch);
	}

	private void analyzeNullCheck(ClassContext classContext, Method method, IsNullValueDataflow invDataflow,
			BasicBlock basicBlock)
		throws DataflowAnalysisException, CFGBuilderException {
		// Look for null checks where the value checked is definitely
		// null or null on some path.

		InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
		Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();

		// Get the stack values at entry to the null check.
		IsNullValueFrame frame = invDataflow.getStartFact(basicBlock);
		if (!frame.isValid())
			return;


		// Could the reference be null?
		IsNullValue refValue = frame.getInstance(exceptionThrower, classContext.getConstantPoolGen());
		if (DEBUG) {
			System.out.println("For basic block " + basicBlock + " value is " + refValue);
		}
		if (!refValue.mightBeNull())
			return;

		// if (!refValue.isDefinitelyNull()) return;
		// Get the value number
		ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getStartFact(basicBlock);
		if (!vnaFrame.isValid())
			return;
		ValueNumber valueNumber = vnaFrame.getInstance(exceptionThrower, classContext.getConstantPoolGen());
		Location location = new Location(exceptionThrowerHandle, basicBlock);
		if (DEBUG) System.out.println("Warning: VN " + valueNumber + " invf: " + frame + " @ " + location);

		// Issue a warning
		collector.foundNullDeref(classContext, location, valueNumber, refValue, vnaFrame);
	}

	/**
	 * @deprecated Use {@link ValueNumberSourceInfo#findXFieldFromValueNumber(Method,Location,ValueNumber,ValueNumberFrame)} instead
	 */
	@Deprecated
	public static XField findXFieldFromValueNumber(Method method,
			Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		return ValueNumberSourceInfo.findXFieldFromValueNumber(method, location, valueNumber, vnaFrame);
	}

	/**
	 * @deprecated Use {@link ValueNumberSourceInfo#findFieldAnnotationFromValueNumber(Method,Location,ValueNumber,ValueNumberFrame)} instead
	 */
	@Deprecated
	public static FieldAnnotation findFieldAnnotationFromValueNumber(
			Method method, Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		return ValueNumberSourceInfo.findFieldAnnotationFromValueNumber(method, location, valueNumber, vnaFrame);
	}

	/**
	 * @deprecated Use {@link ValueNumberSourceInfo#findLocalAnnotationFromValueNumber(Method,Location,ValueNumber,ValueNumberFrame)} instead
	 */
	@Deprecated
	public static LocalVariableAnnotation findLocalAnnotationFromValueNumber(
			Method method, Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		return ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location, valueNumber, vnaFrame);
	}

	/**
	 * @param method
	 *            TODO
	 * @param location
	 * @param valueNumber
	 * @param vnaFrame
	 * @return the annotation
	 * @deprecated Use {@link ValueNumberSourceInfo#findAnnotationFromValueNumber(Method,Location,ValueNumber,ValueNumberFrame)} instead
	 */
	@Deprecated
	public static BugAnnotation findAnnotationFromValueNumber(Method method,
			Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		return ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, valueNumber, vnaFrame);
	}

	private static int getLineNumber(Method method, InstructionHandle handle) {
		LineNumberTable table = method.getCode().getLineNumberTable();
		if (table == null)
			return -1;
		return table.getSourceLine(handle.getPosition());
	}
}
