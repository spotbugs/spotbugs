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

package edu.umd.cs.findbugs.ba.obl;

import edu.umd.cs.findbugs.ba.Path;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

	private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug");

	private MethodGen methodGen;
	private ObligationFactory factory;
	private ObligationPolicyDatabase database;
	private IErrorLogger errorLogger;
	private InstructionActionCache actionCache;

	/**
	 * Constructor.
	 * 
	 * @param dfs       a DepthFirstSearch on the method to be analyzed
	 * @param methodGen the MethodGen of the method being analyzed
	 * @param factory   the ObligationFactory defining the obligation types
	 * @param database  the PolicyDatabase defining the methods which
	 *                  add and delete obligations
	 * @param errorLogger callback to use when reporting
	 *                              missing classes
	 */
	public ObligationAnalysis(
			DepthFirstSearch dfs,
			MethodGen methodGen,
			ObligationFactory factory,
			ObligationPolicyDatabase database,
			IErrorLogger errorLogger) {
		super(dfs);
		this.methodGen = methodGen;
		this.factory = factory;
		this.database = database;
		this.errorLogger = errorLogger;
		this.actionCache = new InstructionActionCache(database);
	}

	public InstructionActionCache getActionCache() {
		return actionCache;
	}

	public StateSet createFact() {
		return new StateSet(factory);
	}

	@Override
	public boolean isFactValid(StateSet fact) {
		return fact.isValid();
	}
	
	@Override
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, StateSet fact)
			throws DataflowAnalysisException {
		//
		// FIXME: it would be better to do this lookup once per Location
		// and cache the result.  For now, just repeat the lookup
		// every time.
		//

//		ArrayList<ObligationPolicyDatabaseAction> actionList = new ArrayList<ObligationPolicyDatabaseAction>();
//		database.getActions(handle, methodGen.getConstantPool(), actionList);
		Collection<ObligationPolicyDatabaseAction> actionList = actionCache.getActions(handle, methodGen.getConstantPool());
		for (ObligationPolicyDatabaseAction action : actionList) {
			action.apply(fact, basicBlock.getLabel());
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transfer(edu.umd.cs.findbugs.ba.BasicBlock, org.apache.bcel.generic.InstructionHandle, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void transfer(BasicBlock basicBlock, @CheckForNull  InstructionHandle end, StateSet start, StateSet result) throws DataflowAnalysisException {
		super.transfer(basicBlock, end, start, result);
		endTransfer(basicBlock, end, result);
	}

	private void endTransfer(BasicBlock basicBlock, @CheckForNull InstructionHandle end, StateSet result)
			throws DataflowAnalysisException {
		// Append this block id to the Paths of all States
		for (Iterator<State> i = result.stateIterator(); i.hasNext(); ) {
			State state = i.next();
			state.getPath().append(basicBlock.getLabel());
		}
	}

	@Override
	public void edgeTransfer(Edge edge, StateSet fact) throws DataflowAnalysisException {
		//
		// Ignore all exception edges except those on which
		// checked exceptions are thrown.
		//
		if (edge.isExceptionEdge() && !edge.isFlagSet(Edge.CHECKED_EXCEPTIONS_FLAG)) {
			fact.setTop();
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
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(edu.umd.cs.findbugs.ba.obl.StateSet)
	 */
	public void makeFactTop(StateSet fact) {
		fact.setTop();
	}

	public boolean isTop(StateSet fact) {
		return fact.isTop();
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
		final StateSet inputFact = fact;

		// Handle easy top and bottom cases
		if (inputFact.isTop() || result.isBottom()) {
			// Nothing to do
		} else if (inputFact.isBottom() || result.isTop()) {
			copy(inputFact, result);
		} else {
			// We will destructively replace the state map of the result fact
			// we're building.
			final Map<ObligationSet, State> updatedStateMap = result.createEmptyMap();

			// Build a Set of all ObligationSets.
			Set<ObligationSet> allObligationSets = new HashSet<ObligationSet>();
			allObligationSets.addAll(inputFact.getAllObligationSets());
			allObligationSets.addAll(result.getAllObligationSets());
			
			// Go through set of all ObligationsSets.
			// When both inputFact and result fact have a State
			// with a common ObligationSet, we combine them into
			// a single State.
			for (Iterator<ObligationSet> i = allObligationSets.iterator(); i.hasNext(); ) {
				ObligationSet obligationSet = i.next();
				
				State stateInInputFact = inputFact.getStateWithObligationSet(obligationSet);
				State stateInResultFact = result.getStateWithObligationSet(obligationSet);
				
				State stateToAdd;
				
				if (stateInInputFact != null && stateInResultFact != null) {
					// Combine the two states,
					// using the shorter path as the basis
					// of the new state's path.
					// If both paths are the same length, we arbitrarily choose
					// the path from the result fact.
					Path path = stateInResultFact.getPath();
					if (stateInInputFact.getPath().getLength() < path.getLength()) {
						path = stateInInputFact.getPath();
					}
					
					stateToAdd = new State(factory);
					stateToAdd.getObligationSet().copyFrom(obligationSet);
					stateToAdd.getPath().copyFrom(path);
				} else if (stateInInputFact != null) {
					stateToAdd = stateInInputFact.duplicate();
				} else {
//					if (stateInResultFact == null ) {
//						System.out.println("Missing ObligationSet : " + obligationSet);
//						System.out.println("  input fact : " + inputFact);
//						System.out.println("  result fact: " + result);
//					}
					stateToAdd = stateInResultFact.duplicate();
				}
				
				updatedStateMap.put(stateToAdd.getObligationSet(), stateToAdd);
			}
			
			result.replaceMap(updatedStateMap);
		}
	}
}

// vim:ts=4
