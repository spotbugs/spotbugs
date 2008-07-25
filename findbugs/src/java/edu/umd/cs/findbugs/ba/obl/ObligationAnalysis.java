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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

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
	private static final boolean DEBUG_NULL_CHECK = SystemProperties.getBoolean("oa.debug.nullcheck");

	private XMethod xmethod;
	private ConstantPoolGen cpg;
	private ObligationFactory factory;
	private ObligationPolicyDatabase database;
	private TypeDataflow typeDataflow;
	private IsNullValueDataflow invDataflow;
	private IErrorLogger errorLogger;
	private InstructionActionCache actionCache;
	private StateSet cachedEntryFact;

	/**
	 * Constructor.
	 * 
	 * @param dfs       a DepthFirstSearch on the method to be analyzed
	 * @param xmethod   method to analyze
	 * @param cpg       ConstantPoolGen of the method to be analyzed
	 * @param factory   the ObligationFactory defining the obligation types
	 * @param database  the PolicyDatabase defining the methods which
	 *                  add and delete obligations
	 * @param errorLogger callback to use when reporting
	 *                              missing classes
	 */
	public ObligationAnalysis(
			DepthFirstSearch dfs,
			XMethod xmethod,
			ConstantPoolGen cpg,
			ObligationFactory factory,
			ObligationPolicyDatabase database,
			TypeDataflow typeDataflow,
			IsNullValueDataflow invDataflow,
			IErrorLogger errorLogger) {
		super(dfs);
		this.xmethod = xmethod;
		this.cpg = cpg;
		this.factory = factory;
		this.database = database;
		this.typeDataflow = typeDataflow;
		this.invDataflow = invDataflow;
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
		Collection<ObligationPolicyDatabaseAction> actionList = actionCache.getActions(handle, cpg);
		if (DEBUG && actionList.size() > 0) {
			System.out.println("Applying actions at " + handle + " to " + fact);
		}
		for (ObligationPolicyDatabaseAction action : actionList) {
			if (DEBUG) {
				System.out.print("  " + action + "...");
			}
			action.apply(fact, basicBlock.getLabel());
			if (DEBUG) {
				System.out.println(fact);
			}
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
		if (edge.isExceptionEdge()) {
			if (!edge.isFlagSet(Edge.CHECKED_EXCEPTIONS_FLAG)) {
				//
				// Ignore all exception edges except those on which
				// checked exceptions are thrown.
				//
				fact.setTop();
			} else {
				//
				// If the edge is an exception thrown from a method that
				// tries to discharge an obligation, then that obligation needs to
				// be removed from all states.
				//
				BasicBlock sourceBlock = edge.getSource();
				InstructionHandle handle = sourceBlock.getExceptionThrower();

				// Apply only the actions which delete obligations
				Collection<ObligationPolicyDatabaseAction> actions = actionCache.getActions(handle, cpg);
				for (ObligationPolicyDatabaseAction action : actions) {
					if (action.getActionType() == ObligationPolicyDatabaseActionType.DEL) {
						action.apply(fact, edge.getTarget().getLabel());
					}
				}
			}
		}

		// If the edge is from a reference comparision
		// which has established that a reference of an obligation type
		// is null, then we remove one occurrence of that type of
		// obligation from all states.
		if (isPossibleIfComparison(edge)) {
			Obligation comparedObligation = comparesObligationTypeToNull(edge);
			if (comparedObligation != null/* && comparedObligation.equals(possiblyLeakedObligation)*/) {
				if (DEBUG_NULL_CHECK) {
					System.out.println("Deleting " + comparedObligation.toString() +
						" on edge from comparision " + edge.getSource().getLastInstruction());
				}
				fact.deleteObligation(comparedObligation, edge.getTarget().getLabel());
			}
		}
	}
	
	private boolean isPossibleIfComparison(Edge edge) {
		return edge.getType() == EdgeTypes.IFCMP_EDGE || edge.getType() == EdgeTypes.FALL_THROUGH_EDGE;
	}

	private Obligation comparesObligationTypeToNull(Edge edge)
		throws DataflowAnalysisException {
		BasicBlock sourceBlock = edge.getSource();
		InstructionHandle last = sourceBlock.getLastInstruction();
		if (last == null) {
			if (DEBUG_NULL_CHECK) {
				System.out.println("no last instruction in source block of " + edge + " ???");
			}
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
			errorLogger.reportMissingClass(e);
			throw new DataflowAnalysisException(
				"Subtype query failed during ObligationAnalysis", e);
		}

	}

	private Type nullCheck(TypeDataflow typeDataflow, short opcode, Edge edge, InstructionHandle last, BasicBlock sourceBlock) throws DataflowAnalysisException {
		if (DEBUG_NULL_CHECK) {
			System.out.println("checking for nullcheck on edge " + edge);
		}
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
		if (cachedEntryFact == null) {
			cachedEntryFact = new StateSet(factory);
			
			//
			// Initial state - create obligations for each parameter
			// marked with a @WillClose annotation.
			//
			
			State state = new State(factory);
			ClassDescriptor willClose = DescriptorFactory.createClassDescriptor("javax/annotation/WillClose");
			Obligation[] paramObligations = factory.getParameterObligationTypes(xmethod);

			for (int i = 0; i < paramObligations.length; i++) {
				if (paramObligations[i] != null && xmethod.getParameterAnnotation(i, willClose) != null) {
					state.getObligationSet().add(paramObligations[i]);
				}
			}
			
			// Add the state
			HashMap<ObligationSet, State> map = new HashMap<ObligationSet, State>();
			map.put(state.getObligationSet(), state);
			cachedEntryFact.replaceMap(map);
		}
		
		fact.copyFrom(cachedEntryFact);
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
		
		if (DEBUG && inputFact.isValid()) {
			for (Iterator<State> i = inputFact.stateIterator(); i.hasNext();) {
				State state = i.next();
				Path path = state.getPath();
				if (path.getLength() > 0) {
					if (path.getBlockIdAt(path.getLength() - 1) != edge.getSource().getLabel()) {
						throw new IllegalStateException("on edge " + edge + ": state " + state + " missing source label in path");
					}
				}
			}
		}

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
					stateToAdd = stateInResultFact.duplicate();
				}
				
				updatedStateMap.put(stateToAdd.getObligationSet(), stateToAdd);
			}
			
			result.replaceMap(updatedStateMap);
		}
	}
}

// vim:ts=4
