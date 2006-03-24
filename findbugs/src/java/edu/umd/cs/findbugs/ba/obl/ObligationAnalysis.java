/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.obl;

import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * Dataflow analysis to track obligations (i/o streams and other
 * resources which must be closed).
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 *
 * @author David Hovemeyer
 */
public class ObligationAnalysis
	extends ForwardDataflowAnalysis<StateSet> {
	
	private static final boolean DEBUG = Boolean.getBoolean("oa.debug");

	private TypeDataflow typeDataflow;
	private MethodGen methodGen;
	private ObligationFactory factory;
	private PolicyDatabase database;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	/**
	 * Constructor.
	 * 
	 * @param dfs       a DepthFirstSearch on the method to be analyzed
	 * @param methodGen the MethodGen of the method being analyzed
	 * @param factory   the ObligationFactory defining the obligation types
	 * @param database  the PolicyDatabase defining the methods which
	 *                  add and delete obligations
	 * @param lookupFailureCallback callback to use when reporting
	 *                              missing classes
	 */
	public ObligationAnalysis(
			DepthFirstSearch dfs,
			TypeDataflow typeDataflow,
			MethodGen methodGen,
			ObligationFactory factory,
			PolicyDatabase database,
			RepositoryLookupFailureCallback lookupFailureCallback) {
		super(dfs);
		this.typeDataflow = typeDataflow;
		this.methodGen = methodGen;
		this.factory = factory;
		this.database = database;
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public StateSet createFact() {
		return new StateSet();
	}

	@Override
         public boolean isFactValid(StateSet fact) {
		return fact.isValid();
	}

	@Override
         public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, StateSet fact)
			throws DataflowAnalysisException {
		
		Obligation obligation;
		
		if ((obligation = addsObligation(handle)) != null) {
			// Add obligation to all states
			if (DEBUG) { System.out.println("Adding obligation " + obligation.toString()); }
			fact.addObligation(obligation);
		} else if ((obligation = deletesObligation(handle)) != null) {
			// Delete obligation from all states
			if (DEBUG) { System.out.println("Deleting obligation " + obligation.toString()); }
			deleteObligation(fact, obligation, handle);
		}

	}
	
	@Override
         public void endTransfer(BasicBlock basicBlock, InstructionHandle end, Object result_) throws DataflowAnalysisException {
		StateSet result = (StateSet) result_;
		
		// Append this block id to the Paths of all States
		for (Iterator<State> i = result.stateIterator(); i.hasNext(); ) {
			State state = i.next();
			state.getPath().append(basicBlock.getId());
		}
	}

	private Obligation addsObligation(InstructionHandle handle) {
		return addsOrDeletesObligation(handle, PolicyDatabase.ADD);
	}

	private Obligation deletesObligation(InstructionHandle handle) {
		return addsOrDeletesObligation(handle, PolicyDatabase.DEL);
	}

	private Obligation addsOrDeletesObligation(InstructionHandle handle, int action) {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof InvokeInstruction))
			return null;
		
		InvokeInstruction inv = (InvokeInstruction) ins;
		
		ConstantPoolGen cpg = methodGen.getConstantPool();
		
		String className = inv.getClassName(cpg);
		// FIXME: could prescreen class here...?
		
		String methodName = inv.getName(cpg);
		String signature = inv.getSignature(cpg);
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
		
		if (DEBUG) {
			System.out.println("Checking instruction: " + handle);
			System.out.println("  class    =" + className);
			System.out.println("  method   =" + methodName);
			System.out.println("  signature=" + signature);
		}
		
		try {
			return database.lookup(
				className, methodName, signature, isStatic, action);
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			return null;
		}
		
	}
	
	/**
	 * Delete Obligation from all states, throwing a DataflowAnalysisException
	 * if any of the states doesn't contain that Obligation.
	 * 
	 * @param fact       the StateSet to remove the Obligation from
	 * @param obligation the Obligation
	 * @param handle     the instruction which deletes the obligation
	 * @throw DataflowAnalysisException if any State doesn't contain the obligation
	 */
	private void deleteObligation(StateSet fact, Obligation obligation, InstructionHandle handle)
			throws DataflowAnalysisException {
		try {
			fact.deleteObligation(obligation);
		} catch (NonexistentObligationException e) {
			throw new DataflowAnalysisException(
					"Removing nonexistent obligation of type " + obligation.toString(),
					methodGen, handle, e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(edu.umd.cs.findbugs.ba.obl.StateSet, edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void copy(StateSet src, StateSet dest) {
		dest.copyFrom(src);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void initEntryFact(StateSet fact) throws DataflowAnalysisException {
		fact.initEntryFact(factory);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void initResultFact(StateSet fact) {
		fact.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void makeFactTop(StateSet fact) {
		fact.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(edu.umd.cs.findbugs.ba.obl.StateSet, edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public boolean same(StateSet a, StateSet b) {
		return a.equals(b);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(edu.umd.cs.findbugs.ba.obl.StateSet, edu.umd.cs.findbugs.ba.Edge, edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void meetInto(StateSet fact, Edge edge, StateSet result)
			throws DataflowAnalysisException {

		// If the edge is an exception thrown from a method that
		// tries to discharge an obligation, then that obligation needs to
		// be removed from all states in the input fact.
		if (edge.isExceptionEdge() && fact.isValid()) {
			BasicBlock sourceBlock = edge.getSource();
			InstructionHandle handle = sourceBlock.getExceptionThrower();
			Obligation obligation;
			if ((obligation = deletesObligation(handle)) != null) {
				fact = fact.duplicate();
				deleteObligation(fact, obligation, handle);
			}
		}
		
		// Similarly, if the incoming edge is from a reference comparision
		// which has established that a reference of an obligation type
		// is null, then we remove one occurrence of that type of
		// obligation from all states.
		if (isPossibleIfComparison(edge)) {
			Obligation obligation;
			if ((obligation = comparesObligationTypeToNull(edge)) != null) {
				fact = fact.duplicate();
				if (DEBUG) {
					System.out.println("Deleting " + obligation.toString() +
							" on edge from comparision " + edge.getSource().getLastInstruction());
				}
				deleteObligation(fact, obligation, edge.getSource().getLastInstruction());
			}
		}

		final StateSet inputFact = fact;

		// Handle easy top and bottom cases
		if (inputFact.isTop() || result.isBottom()) {
			// Nothing to do
		} else if (inputFact.isBottom() || result.isTop()) {
			copy(inputFact, result);
		} else {
			// Various things need to happen here
			// - Match up states with equal ObligationSets
			// - Paths with multiple occurences of a program point,
			//   but different obligation sets on different passes
			//   (i.e., obligation created inside a loop but not deleted)
			//   (how do we detect this?)

			// We will destructively replace the state map of the result fact
			// we're building.
			final Map<ObligationSet, State> updatedStateMap = result.createEmptyMap();
			
			// Get all of the States from the input fact that don't
			// have matching states.  These will be copied verbatim
			// into the result fact.
			for (Iterator<State> i = inputFact.stateIterator(); i.hasNext(); ) {
				State otherState = i.next();
				if (result.getStateWithObligationSet(otherState.getObligationSet()) == null) {
					// Input fact has a State with an ObligationSet not in
					// the result fact.  Add a duplicate of it.
					State dup = otherState.duplicate();
					updatedStateMap.put(dup.getObligationSet(), dup);
				}
			}

			// Find states from the input fact that have obligation sets
			// which match a State in the result fact, and combine them
			// into a single State.
			StateSet.StateCallback callback = new StateSet.StateCallback() {
				public void apply(State state) throws NonexistentObligationException {
					// Find state in other fact with same obligation set (if any).
					State matchingState = inputFact.getStateWithObligationSet(state.getObligationSet());
					if (matchingState != null) {
						// Combine the states by using the shorter of the two paths.
						if (state.getPath().getLength() > matchingState.getPath().getLength()) {
							state.getPath().copyFrom(matchingState.getPath());
						}
					}
					updatedStateMap.put(state.getObligationSet(), state);
				}
			};
			
			try {
				result.applyToAllStatesAndUpdateMap(callback, updatedStateMap);
			} catch (NonexistentObligationException e) {
				// This can't happen, since we're not removing an obligation.
				// But we'll propagate the exception just to be cautious.
				throw new DataflowAnalysisException("This shouldn't happen", e);
			}
		}
	}
	
	private boolean isPossibleIfComparison(Edge edge) {
		return edge.getType() == EdgeTypes.IFCMP_EDGE
			|| edge.getType() == EdgeTypes.FALL_THROUGH_EDGE;
	}
	
	private Obligation comparesObligationTypeToNull(Edge edge)
			throws DataflowAnalysisException {
		BasicBlock sourceBlock = edge.getSource();
		InstructionHandle last = sourceBlock.getLastInstruction();
		if (last == null)
			return null;
		
		Type type;
		
		short opcode = last.getInstruction().getOpcode();
		switch (opcode) {
		case Constants.IFNULL:
		case Constants.IFNONNULL:
			if (   (edge.getType() == EdgeTypes.IFCMP_EDGE && opcode == Constants.IFNONNULL)
				|| (edge.getType() == EdgeTypes.FALL_THROUGH_EDGE && opcode == Constants.IFNULL))
				return null;
			
			Location location = new Location(last, sourceBlock);
			TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
			type = typeFrame.getTopValue();
			break;
			
/*
		// FIXME: handle IF_ACMPXX
 		case Constants.IFACMP_EQ:
 		case Constants.IFACMP_NE:
 			// ...
 			break;
 		
 */
		
		default:
			return null;
		}
		
		if (!(type instanceof ObjectType))
			return null;
		
		try {
			return factory.getObligationByType((ObjectType) type);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			throw new DataflowAnalysisException(
					"Subtype query failed during ObligationAnalysis", e);
		}

	}
}

// vim:ts=4
