/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.deref;

import java.util.BitSet;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullConditionDecision;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessProperty;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.AvailableLoad;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.detect.BuildUnconditionalParamDerefDatabase;

/**
 * Dataflow analysis to find values unconditionally derefenced in the future.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalValueDerefAnalysis extends
		BackwardDataflowAnalysis<UnconditionalValueDerefSet> {
	
	public static final boolean DEBUG = SystemProperties.getBoolean("fnd.derefs.debug");
	public static final boolean ASSUME_NONZERO_TRIP_LOOPS = SystemProperties.getBoolean("fnd.derefs.nonzerotrip");
	public static final boolean IGNORE_DEREF_OF_NONNCP = 
		SystemProperties.getBoolean("fnd.derefs.ignorenonNCP", true);

	public static final boolean IGNORE_DEREF_OF_NONNULL = IGNORE_DEREF_OF_NONNCP 
									|| SystemProperties.getBoolean("fnd.derefs.ignorenonnull");
	public static final boolean CHECK_ANNOTATIONS = 
		SystemProperties.getBoolean("fnd.derefs.checkannotations", true);
	public static final boolean CHECK_CALLS = 
		SystemProperties.getBoolean("fnd.derefs.checkcalls", true);
	public static final boolean DEBUG_CHECK_CALLS =
		SystemProperties.getBoolean("fnd.derefs.checkcalls.debug");
	
	private CFG cfg;
	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;
	private AssertionMethods assertionMethods;
	
	private IsNullValueDataflow invDataflow;
	private TypeDataflow typeDataflow;
	
	/**
	 * Constructor.
	 * 
	 * @param rdfs               the reverse depth-first-search (for the block order)
	 * @param dfs TODO
	 * @param cfg                the CFG for the method
	 * @param methodGen          the MethodGen for the method
	 * @param assertionMethods   AssertionMethods for the analyzed class
	 * @param valueNumberFactory the value number factory
	 */
	public UnconditionalValueDerefAnalysis(
			ReverseDepthFirstSearch rdfs,
			DepthFirstSearch dfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow, AssertionMethods assertionMethods
			) {
		super(rdfs, dfs);
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
		this.assertionMethods = assertionMethods;
		if (DEBUG) {
			System.out.println("UnconditionalValueDerefAnalysis analysis " + methodGen.getClassName() + "." + methodGen.getName() + " : " + methodGen.getSignature());
		}
		if (DEBUG && IGNORE_DEREF_OF_NONNULL) {
			System.out.println("** Ignoring dereferences of definitely non-null values");
		}
	}
	
	/**
	 * HACK: use the given is-null dataflow to clear deref sets for
	 * values that are known to be definitely non-null on a branch.
	 * 
	 * @param invDataflow the IsNullValueDataflow to use
	 */
	public void clearDerefsOnNonNullBranches(IsNullValueDataflow invDataflow) {
		this.invDataflow = invDataflow;
	}
	
	/**
	 * 
	 * 
	 */
	public void setTypeDataflow(TypeDataflow typeDataflow) {
		this.typeDataflow= typeDataflow;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#isFactValid(java.lang.Object)
	 */
	@Override
	public boolean isFactValid(UnconditionalValueDerefSet fact) {
		return !fact.isTop() && !fact.isBottom();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org.apache.bcel.generic.InstructionHandle, edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
	 */
	@Override
	public void transferInstruction(InstructionHandle handle,
			BasicBlock basicBlock, UnconditionalValueDerefSet fact)
			throws DataflowAnalysisException {
		
		if (fact.isTop()) return;
		Location location = new Location(handle, basicBlock);
		
		// If this is a call to an assertion method,
		// change the dataflow value to be TOP.
		// We don't want to report future derefs that would
		// be guaranteed only if the assertion methods
		// returns normally.
		// TODO: at some point, evaluate whether we should revisit this
		if (isAssertion(handle) || handle.getInstruction() instanceof ATHROW ) {
			if (DEBUG) System.out.println("MAKING BOTTOM0 AT: " + location);
			fact.clear();
			return;
		}
		
		// Get value number frame
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(location);
		if (!vnaFrame.isValid()) {
			if (DEBUG) System.out.println("MAKING TOP1 AT: " + location);
			// Probably dead code.
			// Assume this location can't be reached.
			makeFactTop(fact);
			return;
		}
		

		// Check for calls to a method that unconditionally dereferences
		// a parameter.  Mark any such arguments as derefs.
		if (CHECK_CALLS && handle.getInstruction() instanceof InvokeInstruction) {
			checkUnconditionalDerefDatabase(location, vnaFrame, fact);
		}
		
		// If this is a method call instruction,
		// check to see if any of the parameters are @NonNull,
		// and treat them as dereferences.
		if (CHECK_ANNOTATIONS && handle.getInstruction() instanceof InvokeInstruction) {
			checkNonNullParams(location, vnaFrame, fact);
		}

		// Check to see if an instance value is dereferenced here
		checkInstance(location, vnaFrame, fact);
		
		if (false) fact.cleanDerefSet(location, vnaFrame);

		if (DEBUG && fact.isTop()) System.out.println("MAKING TOP2 At: " + location);
		
	}

	/**
	 * Check method call at given location to see if it unconditionally
	 * dereferences a parameter.  Mark any such arguments as derefs.
	 * 
	 * @param location the Location of the method call
	 * @param vnaFrame ValueNumberFrame at the Location
	 * @param fact     the dataflow value to modify
	 * @throws DataflowAnalysisException 
	 */
	private void checkUnconditionalDerefDatabase(
			Location location,
			ValueNumberFrame vnaFrame,
			UnconditionalValueDerefSet fact) throws DataflowAnalysisException {
		
		ParameterNullnessPropertyDatabase database =
			AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase();
		if (database == null) {
			if (DEBUG_CHECK_CALLS) {
				System.out.println("no database!");
			}
			return;
		}
		
		InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
		TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
		if (!typeFrame.isValid()) {
			if (DEBUG_CHECK_CALLS) {
				System.out.println("invalid type frame!");
			}
			return;
		}
		
		SignatureParser sigParser = new SignatureParser(inv.getSignature(methodGen.getConstantPool()));
		int numParams = sigParser.getNumParameters();
		
		try {
			Set<JavaClassAndMethod> targetSet = Hierarchy.resolveMethodCallTargets(
					inv,
					typeFrame,
					methodGen.getConstantPool());
			
			if (targetSet.isEmpty()) {
				return;
			}
			
			// Compute the intersection of all properties
			ParameterNullnessProperty derefParamSet = null;
			for (JavaClassAndMethod target : targetSet) {
				if (DEBUG_CHECK_CALLS) {
					System.out.print("Checking " + target + ": ");
				}
				
				ParameterNullnessProperty targetDerefParamSet = database.getProperty(target.toXMethod());
				if (targetDerefParamSet == null) {
					// Hmm...no information for this target.
					// assume it doesn't dereference anything
					if (DEBUG_CHECK_CALLS) {
						System.out.print("==> no information, assume no guaranteed dereferences");
					}
			
					return;
				}
				
				if (DEBUG_CHECK_CALLS) {
					System.out.println("==> " + targetDerefParamSet);
				}
				if (derefParamSet == null) {
					derefParamSet = new ParameterNullnessProperty();
					derefParamSet.copyFrom(targetDerefParamSet);
				} else {
					derefParamSet.intersectWith(targetDerefParamSet);
				}
			}
			
			if (derefParamSet == null || derefParamSet.isEmpty()) {
				return;
			}
			if (DEBUG_CHECK_CALLS) {
				System.out.println("** Summary of call: " + derefParamSet);
			}
			
			IsNullValueFrame invFrame = null;
			if (IGNORE_DEREF_OF_NONNULL && invDataflow != null) {
				invFrame = invDataflow.getFactAtLocation(location);
				if (!invFrame.isValid()) {
					invFrame = null;
				}
			}
			
			for (int i = 0; i < numParams; i++) {
				if (!derefParamSet.isNonNull(i)) {
					continue;
				}
				
				int argSlot = vnaFrame.getArgumentSlot(i, numParams);

				if (invFrame != null) {
					IsNullValue val = invFrame.getValue(argSlot);
					if (val.isDefinitelyNotNull()) {
						continue;
					}
					if (IGNORE_DEREF_OF_NONNCP && !val.isNullOnComplicatedPath()) continue;
				}
				
				fact.addDeref(vnaFrame.getValue(argSlot), location);
				if (DEBUG_CHECK_CALLS ||VERBOSE_NULLARG_DEBUG) {
					System.out.println("Adding deref of " + vnaFrame.getValue(argSlot) + " at location " + location);
					for (JavaClassAndMethod target : targetSet) {

						System.out.print("Checking " + target + ": ");
						ParameterNullnessProperty targetDerefParamSet = database.getProperty(target.toXMethod());
						if (targetDerefParamSet == null) {
							System.out.println(" ==> unknown");
							continue;
						}


						System.out.println("==> " + targetDerefParamSet);

					}

				}
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
	}
	public static final boolean VERBOSE_NULLARG_DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg.verbose");
	
	/**
	 * If this is a method call instruction,
	 * check to see if any of the parameters are @NonNull,
	 * and treat them as dereferences.
	 * 
	 * @param location  the Location of the instruction
	 * @param vnaFrame  the ValueNumberFrame at the Location of the instruction
	 * @param fact      the dataflow value to modify
	 * @throws DataflowAnalysisException
	 */
	private void checkNonNullParams(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact) throws DataflowAnalysisException {
		NullnessAnnotationDatabase database = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase();
		if (database == null) {
			return;
		}

		InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction(); 
		XMethod called = XFactory.createXMethod(
				inv,
				methodGen.getConstantPool());
		SignatureParser sigParser = new SignatureParser(called.getSignature());
		int numParams = sigParser.getNumParameters();

		for (int i = 0; i < numParams; i++) {
			if (IGNORE_DEREF_OF_NONNULL
					&& invDataflow != null) {
				IsNullValueFrame invFrame = invDataflow.getFactAtLocation(location);
				if (isNonNullValue(invFrame, invFrame.getArgumentSlot(i, numParams))) {
					continue;
				}				
			}
			if (database.parameterMustBeNonNull(called, i)) {
				// Get the corresponding value number
				ValueNumber vn = vnaFrame.getArgument(inv, methodGen.getConstantPool(), i, numParams);
				fact.addDeref(vn, location);
			}
		}
	}

	/**
	 * Check to see if the instruction has a null check associated with it,
	 * and if so, add a dereference.
	 * 
	 * @param location the Location of the instruction
	 * @param vnaFrame ValueNumberFrame at the Location of the instruction
	 * @param fact     the dataflow value to modify
	 * @throws DataflowAnalysisException
	 */
	private void checkInstance(
			Location location,
			ValueNumberFrame vnaFrame,
			UnconditionalValueDerefSet fact) throws DataflowAnalysisException {
		// See if this instruction has a null check.
		// If it does, the fall through predecessor will be
		// identify itself as the null check.
		if (!location.isFirstInstructionInBasicBlock()) {
			return;
		}
		BasicBlock fallThroughPredecessor =
			cfg.getPredecessorWithEdgeType(location.getBasicBlock(), EdgeTypes.FALL_THROUGH_EDGE);
		if (fallThroughPredecessor == null || !fallThroughPredecessor.isNullCheck()) {
			return;
		}
		
		// Get the null-checked value
		ValueNumber vn = vnaFrame.getInstance(location.getHandle().getInstruction(), methodGen.getConstantPool()); 
		
		// Ignore dereferences of this
		if (!methodGen.isStatic()) {
			ValueNumber v = vnaFrame.getValue(0);
			if (v.equals(vn)) return;
		}
		if (vn.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) return;

		IsNullValueFrame startFact = null;

		if (invDataflow != null) {
			startFact = invDataflow.getStartFact(fallThroughPredecessor);
		}

		// Ignore dereferences of values that are definitely non-null
		if (IGNORE_DEREF_OF_NONNULL
				&& invDataflow != null
				&& isDerefOfNonNullValue(location, startFact)) {
			return;
		}
		
		if (IGNORE_DEREF_OF_NONNCP
				&& invDataflow != null
				&& !isDerefOfNullOnComplexPathValue(location, startFact)) {
			return;
		}
		
		if (DEBUG) {
			System.out.println("FOUND GUARANTEED DEREFERENCE");
			System.out.println("Load: " + vnaFrame.getLoad(vn));
			System.out.println("Pred: " + fallThroughPredecessor);
			System.out.println("startFact: " + startFact);
			System.out.println("Location: " + location);
			System.out.println("Value number frame: " + vnaFrame);
			System.out.println("Dereferenced valueNumber: " + vn);
			System.out.println("invDataflow: " + startFact);
			System.out.println("IGNORE_DEREF_OF_NONNCP: " + IGNORE_DEREF_OF_NONNCP);
			System.out.println("IGNORE_DEREF_OF_NONNULL: " + IGNORE_DEREF_OF_NONNULL);
			System.out.println("isNonNull: " + isDerefOfNonNullValue(location, startFact));
			System.out.println("isDerefOfNullOnComplexPathValue: " + isDerefOfNullOnComplexPathValue(location, startFact));

			
		}
		// Mark the value number as being dereferenced at this location
		fact.addDeref(vn, location);
	}

	/**
	 * Determine whether instruction at given Location is a dereference of
	 * a definitely non-null value.
	 * 
	 * @param locationOfDeref     the dereference instruction Location
	 * @param invFrameAtNullCheck the IsNullValueFrame at the location of the null check
	 * @return true if the instruction at the location is a dereference of a definitely
	 *          non-null value, false otherwise
	 * @throws DataflowAnalysisException
	 */
	private boolean isDerefOfNonNullValue(Location locationOfDeref, IsNullValueFrame invFrameAtNullCheck)
			throws DataflowAnalysisException {
		if (!invFrameAtNullCheck.isValid()) {
			// Probably dead code
			return false;
		}
		
		int instance = invFrameAtNullCheck.getInstanceSlot(
				locationOfDeref.getHandle().getInstruction(),
				methodGen.getConstantPool());
		return isNonNullValue(invFrameAtNullCheck, instance);
	}

	/**
	 * Determine whether instruction at given Location is a dereference of
	 * a value null on a complex path
	 * 
	 * @param locationOfDeref     the dereference instruction Location
	 * @param invFrameAtNullCheck the IsNullValueFrame at the location of the null check
	 * @return true if the instruction at the location is a dereference of a definitely
	 *          non-null value, false otherwise
	 * @throws DataflowAnalysisException
	 */
	private boolean isDerefOfNullOnComplexPathValue(Location locationOfDeref, IsNullValueFrame invFrameAtNullCheck)
			throws DataflowAnalysisException {
		if (!invFrameAtNullCheck.isValid()) {
			// Probably dead code
			return false;
		}
		
		int instance = invFrameAtNullCheck.getInstanceSlot(
				locationOfDeref.getHandle().getInstruction(),
				methodGen.getConstantPool());
		return isNullOnComplexPath(invFrameAtNullCheck, instance);
	}
	/**
	 * Return whether or not given slot in given is-null frame
	 * is definitely non-null.
	 * 
	 * @param invFrame an IsNullValueFrame
	 * @param slot     slot in the frame
	 * @return true if value in the slot is definitely non-null, false otherwise
	 */
	private boolean isNonNullValue(IsNullValueFrame invFrame, int slot) {
		if (invFrame == null || !invFrame.isValid()) {
			return false;
		}
		return invFrame.getValue(slot).isDefinitelyNotNull();
	}
	/**
	 * Return whether or not given slot in given is-null frame
	 * is definitely non-null.
	 * 
	 * @param invFrame an IsNullValueFrame
	 * @param slot     slot in the frame
	 * @return true if value in the slot is null on a complicated path
	 */
	private boolean isNullOnComplexPath(IsNullValueFrame invFrame, int slot) {
		if (invFrame == null || !invFrame.isValid()) {
			return false;
		}
		IsNullValue value = invFrame.getValue(slot);
		return value.isNullOnComplicatedPath();
	}
	/**
	 * Return whether or not given instruction is an assertion.
	 * 
	 * @param handle the instruction
	 * @return true if instruction is an assertion, false otherwise
	 */
	private boolean isAssertion(InstructionHandle handle) {
		return handle.getInstruction() instanceof InvokeInstruction
				&& assertionMethods.isAssertionCall((InvokeInstruction) handle.getInstruction());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	public void copy(UnconditionalValueDerefSet source, UnconditionalValueDerefSet dest) {
		dest.makeSameAs(source);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
	 */
	public UnconditionalValueDerefSet createFact() {
		return new UnconditionalValueDerefSet(vnaDataflow.getAnalysis().getNumValuesAllocated());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(UnconditionalValueDerefSet result)
			throws DataflowAnalysisException {
		result.clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(java.lang.Object)
	 */
	public void initResultFact(UnconditionalValueDerefSet result) {
		result.setIsTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
	 */
	public void makeFactTop(UnconditionalValueDerefSet fact) {
		fact.setIsTop();
	}

	public boolean isTop(UnconditionalValueDerefSet fact) {
		return fact.isTop();
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object, edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void meetInto(UnconditionalValueDerefSet fact, Edge edge,
			UnconditionalValueDerefSet result) throws DataflowAnalysisException {
		
		if (ignoreThisEdge(edge)) {
			return;
		}
		
		ValueNumber knownNonnullOnBranch = null;
		// Edge transfer function
		if (isFactValid(fact)) {
			fact = propagateDerefSetsToMergeInputValues(fact, edge);
			if (invDataflow != null) {
				knownNonnullOnBranch = findValueKnownNonnullOnBranch(fact, edge);
				if (knownNonnullOnBranch != null) {
					fact = duplicateFact(fact);
					fact.clearDerefSet(knownNonnullOnBranch);
				}
			}
		}
		boolean isBackEdge = edge.isBackwardInBytecode();
		boolean sourceIsTopOfLoop = edge.sourceIsTopOfLoop(ClassContext.getLoopExitBranches(methodGen));
		if (sourceIsTopOfLoop && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE)
			isBackEdge = true;
		if (false && (edge.getType() == EdgeTypes.IFCMP_EDGE || sourceIsTopOfLoop)) {
			System.out.println("Meet into " + edge);
			System.out.println("  foo2: " + sourceIsTopOfLoop);
			System.out.println("  getType: " + edge.getType() );
		    System.out.println("  Backedge according to bytecode: " + isBackEdge);
		    System.out.println("  Fact hashCode: " + System.identityHashCode(result));
		    System.out.println("  Initial fact: " + result);
		    System.out.println("  Edge fact: " + fact);
		}
		if (result.isTop() || fact.isBottom()) {
			// Make result identical to other fact
			copy(fact, result);
			if (ASSUME_NONZERO_TRIP_LOOPS && isBackEdge && !fact.isTop())
				result.resultsFromBackEdge = true;
		} else if (ASSUME_NONZERO_TRIP_LOOPS && isBackEdge && !fact.isTop()) {
			result.unionWith(fact, vnaDataflow.getAnalysis().getFactory());
			result.resultsFromBackEdge = true;
			if (DEBUG) {
				System.out.println("\n Forcing union of " +  System.identityHashCode(result) + " due to backedge info");
				System.out.println("  result: " +  result);
			}
			
		} else if (result.isBottom() || fact.isTop()) {
			// No change in result fact
		} else {
			// Dataflow merge
			// (intersection of unconditional deref values)
			if (ASSUME_NONZERO_TRIP_LOOPS && result.resultsFromBackEdge) {
				result.backEdgeUpdateCount++;
				if (result.backEdgeUpdateCount < 10) {
					if (DEBUG) System.out.println("\n Union update of " +  System.identityHashCode(result) + " due to backedge info");
					result.unionWith(fact, vnaDataflow.getAnalysis().getFactory());
					return;
				}
			}
			result.mergeWith(fact, knownNonnullOnBranch, vnaDataflow.getAnalysis().getFactory());
			if (DEBUG) {
				System.out.println("  updated: " + System.identityHashCode(result));
				 System.out.println("  result: " +  result);
				 return;
			}
		}
		if (DEBUG && isBackEdge && edge.getType() == EdgeTypes.IFCMP_EDGE) {
		 System.out.println("  result: " +  result);
		}
	}

	/**
	 * Find out if any VNs in the source block
	 * contribute to unconditionally dereferenced VNs in the
	 * target block.  If so, the VN in the source block is
	 * also unconditionally dereferenced, and we must propagate
	 * the target VN's dereferences.
	 *  
	 * @param fact a dataflow value
	 * @param edge edge to check for merge input values
	 * @return possibly-modified dataflow value
	 */
	private  UnconditionalValueDerefSet propagateDerefSetsToMergeInputValues(
			UnconditionalValueDerefSet fact, Edge edge) {
		
		ValueNumberFrame blockValueNumberFrame =
			vnaDataflow.getResultFact(edge.getSource());
		ValueNumberFrame targetValueNumberFrame =
			vnaDataflow.getStartFact(edge.getTarget());

		UnconditionalValueDerefSet originalFact = fact;
		fact = duplicateFact(fact);

		if (blockValueNumberFrame.isValid() && targetValueNumberFrame.isValid() &&
				blockValueNumberFrame.getNumSlots() == targetValueNumberFrame.getNumSlots()) {
			if (DEBUG) {
				System.out.println("** Valid VNA frames for " + edge);
				System.out.println("** Block : " + blockValueNumberFrame);
				System.out.println("** Target: " + targetValueNumberFrame);
			}

			for (int i = 0; i < blockValueNumberFrame.getNumSlots(); i++) {
				ValueNumber blockVN = blockValueNumberFrame.getValue(i);
				ValueNumber targetVN = targetValueNumberFrame.getValue(i);
				fact.clearDerefSet(blockVN);
				if (originalFact.isUnconditionallyDereferenced(targetVN))
					fact.setDerefSet(blockVN, originalFact.getUnconditionalDerefLocationSet(targetVN));

			} // for all slots
	
			for(ValueNumber blockVN : blockValueNumberFrame.valueNumbersForLoads()) {
				AvailableLoad load = blockValueNumberFrame.getLoad(blockVN);
				if (load == null) continue;
				ValueNumber [] targetVNs = targetValueNumberFrame.getAvailableLoad(load);
				if (targetVNs != null)
					for(ValueNumber targetVN : targetVNs) 
						if (targetVN.hasFlag(ValueNumber.PHI_NODE) && fact.isUnconditionallyDereferenced(targetVN)
								&& !fact.isUnconditionallyDereferenced(blockVN)) {
							//  Block VN is also dereferenced unconditionally.
							AvailableLoad targetLoad = targetValueNumberFrame.getLoad(targetVN);
							if (!load.equals(targetLoad)) continue;
							if (DEBUG) {
								System.out.println("** Copy vn derefs for " + load +" from " + targetVN + 
										" --> " + blockVN);
								System.out.println("** block phi for " +  System.identityHashCode(blockValueNumberFrame)
										+ "is " + blockValueNumberFrame.phiNodeForLoads);
								System.out.println("** target phi for " +  System.identityHashCode(targetValueNumberFrame)
										+ "is " + targetValueNumberFrame.phiNodeForLoads);
							}
							fact.setDerefSet(blockVN, fact.getUnconditionalDerefLocationSet(targetVN));

						}

			}
		}
		if (DEBUG) {
			System.out.println("Target VNF: " + targetValueNumberFrame);
			System.out.println("Block VNF: " + blockValueNumberFrame);
			System.out.println("fact: " + fact);
		}
		fact.cleanDerefSet(null, blockValueNumberFrame);
		return fact;
	}

	/**
	 * Return a duplicate of given dataflow fact.
	 * 
	 * @param fact a dataflow fact
	 * @return a duplicate of the input dataflow fact
	 */
	private UnconditionalValueDerefSet duplicateFact(UnconditionalValueDerefSet fact) {
		UnconditionalValueDerefSet copyOfFact = createFact();
		copy(fact, copyOfFact);
		fact = copyOfFact;
		return fact;
	}

	/**
	 * Clear deref sets of values if this edge is the non-null branch
	 * of an if comparison.
	 * 
	 * @param fact a datflow fact
	 * @param edge edge to check
	 * @return possibly-modified dataflow fact
	 */
	private @CheckForNull ValueNumber findValueKnownNonnullOnBranch(
			UnconditionalValueDerefSet fact, Edge edge) {
		
		IsNullValueFrame invFrame = invDataflow.getResultFact(edge.getSource());
		if (!invFrame.isValid()) {
			return null;
		}
		IsNullConditionDecision decision = invFrame.getDecision();
		if (decision == null) {
			return null;
		}
		
		IsNullValue inv = decision.getDecision(edge.getType());
		if (inv == null || !inv.isDefinitelyNotNull()) {
			return null;
		}
		ValueNumber value = decision.getValue();
		if (DEBUG) {
			System.out.println("Value number " + value + " is known nonnull on " + edge);
		}
		
		return value;
	}

	/**
	 * Determine whether dataflow should be propagated on given edge.
	 * 
	 * @param edge the edge
	 * @return true if dataflow should be propagated on the edge, false otherwise
	 */
	private boolean ignoreThisEdge(Edge edge) {
		return edge.isExceptionEdge();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object, java.lang.Object)
	 */
	public boolean same(UnconditionalValueDerefSet fact1, UnconditionalValueDerefSet fact2) {
		return fact1.resultsFromBackEdge || fact1.isSameAs(fact2);
	}

	@Override
	public void startIteration() {
		// System.out.println("analysis iteration in " + methodGen.getClassName() + " on " + methodGen.toString());
	}
	
	@Override
	public int getLastUpdateTimestamp(UnconditionalValueDerefSet fact) {
		return fact.getLastUpdateTimestamp();
	}
	@Override
	public void setLastUpdateTimestamp(UnconditionalValueDerefSet fact, int lastUpdate) {
		fact.setLastUpdateTimestamp(lastUpdate);
	}
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + UnconditionalValueDerefAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}
		
		DataflowTestDriver<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis> driver =
			new DataflowTestDriver<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis>() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext, org.apache.bcel.classfile.Method)
			 */
			@Override
			public Dataflow<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis> createDataflow(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getUnconditionalValueDerefDataflow(method);
			}
		};
		if (SystemProperties.getBoolean("forwardcfg")) {
			driver.overrideIsForwards();
		}
		driver.execute(args[0]);
	}
}