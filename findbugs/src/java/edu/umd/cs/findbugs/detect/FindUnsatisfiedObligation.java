/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005,2008 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.util.Iterator;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationAcquiredOrReleasedInLoopException;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.Path;
import edu.umd.cs.findbugs.ba.obl.State;
import edu.umd.cs.findbugs.ba.obl.StateSet;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * Find unsatisfied obligations in Java methods.
 * Examples: open streams, open database connections, etc.
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 * 
 * @author David Hovemeyer
 */
public class FindUnsatisfiedObligation extends CFGDetector {

	private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug");
	private static final String DEBUG_METHOD = SystemProperties.getProperty("oa.method");
	private static final boolean DEBUG_NULL_CHECK = SystemProperties.getBoolean("oa.debug.nullcheck");

	private BugReporter bugReporter;
	private ObligationPolicyDatabase database;

	public FindUnsatisfiedObligation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (database == null) {
			database = Global.getAnalysisCache().getDatabase(ObligationPolicyDatabase.class);
		}
		
		MethodChecker methodChecker = new MethodChecker(methodDescriptor, cfg);
		methodChecker.analyzeMethod();
	}
	
	/**
	 * A helper class to check a single method for unsatisfied obligations.
	 * Avoids having to pass millions of parameters to each method
	 * (type dataflow, null value dataflow, etc.).
	 */
	private class MethodChecker {
		MethodDescriptor methodDescriptor;
		CFG cfg;
		IAnalysisCache analysisCache;
		ObligationDataflow dataflow;
		ConstantPoolGen cpg;
		IsNullValueDataflow invDataflow;
		TypeDataflow typeDataflow;
		Subtypes2 subtypes2;
		
		MethodChecker(MethodDescriptor methodDescriptor, CFG cfg) {
			this.methodDescriptor = methodDescriptor;
			this.cfg = cfg;
		}
		
		public void analyzeMethod() throws CheckedAnalysisException {
			if (DEBUG) {
				System.out.println("*** Analyzing method " + methodDescriptor);
			}

			if (DEBUG_METHOD != null && !methodDescriptor.getName().equals(DEBUG_METHOD)) {
				return;
			}

			analysisCache = Global.getAnalysisCache();

			// Execute the obligation dataflow analysis
			try {
				dataflow = analysisCache.getMethodAnalysis(ObligationDataflow.class, methodDescriptor);
			} catch (ObligationAcquiredOrReleasedInLoopException e) {
				// It is not possible to analyze this method.
				if (DEBUG) {
					System.out.println("FindUnsatisifedObligation: " + methodDescriptor + ": " + e.getMessage());
				}
				return;
			}

			// We need these to apply the false-positive
			// suppression heuristics.
			cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());
			invDataflow = analysisCache.getMethodAnalysis(IsNullValueDataflow.class, methodDescriptor);
			typeDataflow = analysisCache.getMethodAnalysis(TypeDataflow.class, methodDescriptor);
			subtypes2 = Global.getAnalysisCache().getDatabase(Subtypes2.class);

			//Set<Obligation> leakedObligations = new HashSet<Obligation>();
			Map<Obligation, List<State>> leakedObligations = new HashMap<Obligation, List<State>>();

			//
			// Main loop: looking at the StateSet at the exit block of the CFG,
			// see if there are any states with nonempty obligation sets.
			//
			StateSet factAtExit = dataflow.getStartFact(cfg.getExit());
			for (Iterator<State> i = factAtExit.stateIterator(); i.hasNext();) {
				State state = i.next();

				for (int id = 0; id < database.getFactory().getMaxObligationTypes(); ++id) {
					Obligation obligation = database.getFactory().getObligationById(id);
					// If the raw count produced by the analysis
					// for this obligation type is 0,
					// assume everything is ok on this state's path.
					int rawLeakCount = state.getObligationSet().getCount(id);
					if (rawLeakCount == 0) {
						continue;
					}

					// Apply the false-positive suppression heuristics
					int leakCount;
					try {
						leakCount = getAdjustedLeakCount(state, id);
					} catch (DataflowAnalysisException e) {
						// ignore
						continue;
					} catch (ClassNotFoundException e) {
						// ignore
						continue;
					}

					if (leakCount > 0) {
						//leakedObligations.add(obligation);
						List<State> leakingStateList = leakedObligations.get(obligation);
						if (leakingStateList == null) {
							leakingStateList = new LinkedList<State>();
							leakedObligations.put(obligation, leakingStateList);
						}
						leakingStateList.add(state);
					}
					// TODO: if the leak count is less than 0, then a nonexistent resource was closed
				}
			}

			for (Map.Entry<Obligation, List<State>> entry : leakedObligations.entrySet()) {
				Obligation obligation = entry.getKey();
				List<State> stateList = entry.getValue();
				
				BugInstance bugInstance =
					new BugInstance(FindUnsatisfiedObligation.this, "OBL_UNSATISFIED_OBLIGATION", NORMAL_PRIORITY).addClassAndMethod(methodDescriptor)
					.addClass(obligation.getClassName())
					.describe("CLASS_REFTYPE");
				
				// FIXME:
				// For now, just report the first resource creation point
				// along each path where a leak was detected.
				// In the future, might want to try to convey some path information.
				for (State state : stateList) {
					int blockId = state.getObligationSet().getWhereCreated(obligation);
					if (blockId > 0) {
						BasicBlock creationBlock = cfg.lookupBlockByLabel(blockId);
						
						// Figure out which instruction actually creates
						// the obligation
						for (Iterator<InstructionHandle> i = creationBlock.instructionIterator(); i.hasNext();) {
							InstructionHandle handle = i.next();
							if (dataflow.getAnalysis().getActionCache().addsObligation(handle, cpg, obligation)) {
								bugInstance
									.addSourceLine(methodDescriptor, new Location(handle, creationBlock))
									.describe(SourceLineAnnotation.ROLE_OBLIGATION_CREATED);

							}
						}
					}
							
				}
				
				bugReporter.reportBug(bugInstance);
			}
			// TODO: closing of nonexistent resources

		}

		/**
		 * Get the adjusted leak count for the given State and obligation type.
		 * Use heuristics to account for:
		 * <ul>
		 *   <li> null checks (count the number of times the supposedly leaked obligation
		 *        is compared to null, and subtract those from the leak count)</li>
		 *   <li> field assignments (count number of times obligation type is
		 *        assigned to a field, and subtract those from the leak count)</li>
		 *   <li> return statements (if an instance of the obligation type is
		 *        returned from the method, subtract one from leak count) </li>
		 * </ul>
		 * 
		 * @return the adjusted leak count (positive if leaked obligation, negative if
		 *          attempt to release an un-acquired obligation)
		 */
		private int getAdjustedLeakCount(
			State state, int obligationId) throws DataflowAnalysisException, ClassNotFoundException {
			
			Obligation obligation = database.getFactory().getObligationById(obligationId);
			
			
			int leakCount = state.getObligationSet().getCount(obligationId);

			Path path = state.getPath();
			for (int j = 0; j < path.getLength(); j++) {
				int blockId = path.getBlockIdAt(j);
				BasicBlock basicBlock = cfg.lookupBlockByLabel(blockId);
				assert basicBlock != null;

				for (Iterator<InstructionHandle> i = basicBlock.instructionIterator(); i.hasNext();) {
					Location loc = new Location(i.next(), basicBlock);

					Instruction ins = loc.getHandle().getInstruction();
					short opcode = ins.getOpcode();

					if (opcode == Constants.PUTFIELD || opcode == Constants.PUTSTATIC || opcode == Constants.ARETURN) {
						//
						// A value is being assigned to a field or returned from
						// the method.
						//
						TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
						if (!typeFrame.isValid()) {
							// dead code?
							return 0;
						}
						Type tosType = typeFrame.getTopValue();
						if (tosType instanceof ObjectType && isPossibleInstanceOfObligationType(subtypes2, (ObjectType) tosType, obligation.getType())) {
							// Remove one obligation of this type
							leakCount--;
						}
					}
				}

				if (j < path.getLength() - 1) {
					// Examine the edge from one basic block to the next
					// to see if it's a possible null check,
					// or if the edge is an exception thrown from a method
					// which attempts to discharge the obligation.
					BasicBlock target = cfg.lookupBlockByLabel(path.getBlockIdAt(j + 1));
					assert target != null;
					Edge edge = cfg.lookupEdge(basicBlock, target);
					assert edge != null;

					// If the edge is an exception thrown from a method that
					// tries to discharge an obligation, then that obligation needs to
					// be removed from all states in the input fact.
					if (edge.isExceptionEdge()) {
						BasicBlock sourceBlock = edge.getSource();
						InstructionHandle handle = sourceBlock.getExceptionThrower();
						
						if (dataflow.getAnalysis().getActionCache().deletesObligation(handle, cpg, obligation)) {
							leakCount--;
						}
					}

					// Similarly, if the incoming edge is from a reference comparision
					// which has established that a reference of an obligation type
					// is null, then we remove one occurrence of that type of
					// obligation from all states.
					if (isPossibleIfComparison(edge)) {
						Obligation comparedObligation= comparesObligationTypeToNull(edge);
						if (comparedObligation != null && comparedObligation.equals(obligation)) {
							if (DEBUG) {
								System.out.println("Deleting " + obligation.toString() +
									" on edge from comparision " + edge.getSource().getLastInstruction());
							}
							leakCount--;
						}
					}
				}
			}

			return leakCount;
		}

		private boolean isPossibleInstanceOfObligationType(Subtypes2 subtypes2, ObjectType type, ObjectType obligationType) throws ClassNotFoundException {
			//
			// If we're tracking, e.g., InputStream obligations,
			// and we see a FileInputStream reference being assigned
			// to a field (or returned from a method),
			// then the false-positive supressions heuristic should apply.
			//

			return subtypes2.isSubtype(type, obligationType);
		}

		private boolean isPossibleIfComparison(Edge edge) {
			return edge.getType() == EdgeTypes.IFCMP_EDGE || edge.getType() == EdgeTypes.FALL_THROUGH_EDGE;
		}

		private Obligation comparesObligationTypeToNull(Edge edge)
			throws DataflowAnalysisException {
			BasicBlock sourceBlock = edge.getSource();
			InstructionHandle last = sourceBlock.getLastInstruction();
			if (last == null) {
				return null;
			}
			Type type = null;

			short opcode = last.getInstruction().getOpcode();
			switch (opcode) {
			case Constants.IFNULL:
			case Constants.IFNONNULL:
				type = nullCheck(typeDataflow, opcode, edge, last, sourceBlock);
				break;

			case Constants.IF_ACMPEQ:
			case Constants.IF_ACMPNE:
				type = acmpNullCheck(typeDataflow, invDataflow, opcode, edge, last, sourceBlock);
				break;
			}

			if (type == null || !(type instanceof ObjectType)) {
				return null;
			}

			try {
				// See if the type of value compared to null is an obligation type.
				return database.getFactory().getObligationByType((ObjectType) type);
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
				throw new DataflowAnalysisException(
					"Subtype query failed during ObligationAnalysis", e);
			}

		}

		private Type nullCheck(TypeDataflow typeDataflow, short opcode, Edge edge, InstructionHandle last, BasicBlock sourceBlock) throws DataflowAnalysisException {
			Type type = null;
			if ((opcode == Constants.IFNULL && edge.getType() == EdgeTypes.IFCMP_EDGE) ||
				(opcode == Constants.IFNONNULL && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE)) {
				Location location = new Location(last, sourceBlock);
				TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
				if (typeFrame.isValid()) {
					type = typeFrame.getTopValue();
					if (DEBUG_NULL_CHECK) {
						System.out.println("ifnull comparison of " + type + " to null at " + last);
					}
				}
			}
			return type;
		}

		private Type acmpNullCheck(TypeDataflow typeDataflow, IsNullValueDataflow invDataflow, short opcode, Edge edge, InstructionHandle last, BasicBlock sourceBlock) throws DataflowAnalysisException {
			Type type = null;
			//
			// Make sure that IF a value has been compared to null,
			// this edge is the edge on which the
			// compared value is definitely null.
			//
			if ((opcode == Constants.IF_ACMPEQ && edge.getType() == EdgeTypes.IFCMP_EDGE) ||
				(opcode == Constants.IF_ACMPNE && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE)) {
				//
				// Check nullness and type of the top two stack values.
				//
				Location location = new Location(last, sourceBlock);
				IsNullValueFrame invFrame = invDataflow.getFactAtLocation(location);
				TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
				if (invFrame.isValid() && typeFrame.isValid()) {
					//
					// See if exactly one of the top two stack values is definitely null
					//
					boolean leftIsNull = invFrame.getStackValue(1).isDefinitelyNull();
					boolean rightIsNull = invFrame.getStackValue(0).isDefinitelyNull();

					if ((leftIsNull || rightIsNull) && !(leftIsNull && rightIsNull)) {
						//
						// Now we can determine what type was compared to null.
						//
						type = typeFrame.getStackValue(leftIsNull ? 0 : 1);
						if (DEBUG_NULL_CHECK) {
							System.out.println("acmp comparison of " + type + " to null at " + last);
						}
					}
				}
			}
			return type;
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		// Nothing to do here
	}

}
