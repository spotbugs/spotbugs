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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A dataflow fact used in ObligationAnalysis.
 * It is a set of State objects, plus the additional capability
 * to represent top and bottom elements.
 * 
 * <p>Invariant: no StateSet may contain more than one
 * State with the same ObligationSet.</p>
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 * 
 * @author David Hovemeyer
 */
public class StateSet {
	private boolean isTop;
	private boolean isBottom;
	private Map<ObligationSet, State> stateMap;
	private ObligationFactory factory;

	public StateSet(ObligationFactory factory) {
		this.isTop = this.isBottom = false;
		this.stateMap = new HashMap<ObligationSet, State>();
		this.factory = factory;
	}

	public void setTop() {
		this.isTop = true;
		this.isBottom = false;
		this.stateMap.clear();
	}

	public boolean isTop() {
		return isTop;
	}

	public void setBottom() {
		this.isBottom = true;
		this.isTop = false;
	}

	public boolean isBottom() {
		return this.isBottom;
	}

	public boolean isValid() {
		return !this.isTop && !this.isBottom;
	}

	/**
	 * Return an Iterator over the States in the StateSet.
	 * 
	 * @return an Iterator over the States in the StateSet
	 */
	public Iterator<State> stateIterator() {
		return stateMap.values().iterator();
	}
	
	/**
	 * Get Set of all ObligationsSets in this StateSet.
	 * 
	 * @return Set of all ObligationsSets in this StateSet
	 */
	public Set<ObligationSet> getAllObligationSets() {
		return Collections.unmodifiableSet(stateMap.keySet());
	}

	/**
	 * Get the State which has the given ObligationSet.
	 * Returns null if there is no such state.
	 * 
	 * @param obligationSet we want to get the State with this ObligationSet
	 * @return the State with the given ObligationSet, or null if there is no such State
	 */
	public State getStateWithObligationSet(ObligationSet obligationSet) {
		return stateMap.get(obligationSet);
	}

//	/**
//	 * Initialize this object as the entry fact for a method:
//	 * a single state with empty obligation set and path.
//	 * 
//	 * @param factory the ObligationFactory used for the analysis
//	 */
//	public void initEntryFact(ObligationFactory factory) {
//		this.isTop = this.isBottom = false;
//		this.stateMap.clear();
//
//		// Add initial fact: empty obligations, empty path
//		State initState = new State(factory);
//		this.stateMap.put(initState.getObligationSet(), initState);
//	}

	/**
	 *  Make this StateSet an exact copy of the given StateSet.
	 *  
	 *  @param other a StateSet; this StateSet will be made identical to it
	 */
	public void copyFrom(StateSet other) {
		this.isTop = other.isTop;
		this.isBottom = other.isBottom;
		this.stateMap.clear();
		for (State state : other.stateMap.values()) {
			State dup = state.duplicate();
			this.stateMap.put(dup.getObligationSet(), dup);
		}
	}

	/**
	 * Return an exact deep copy of this StateSet.
	 * 
	 * @return an exact deep copy of this StateSet
	 */
	public StateSet duplicate() {
		StateSet dup = new StateSet(factory);
		dup.copyFrom(this);
		return dup;
	}

	/**
	 * Add an obligation to every State in the StateSet.
	 * 
	 * @param obligation the obligation to add
	 * @param basicBlockId the id of the basic block (path component) adding the obligation
	 */
	public void addObligation(final Obligation obligation, int basicBlockId) throws ObligationAcquiredOrReleasedInLoopException {
		Map<ObligationSet, State> updatedStateMap = new HashMap<ObligationSet, State>();
		for (Iterator<State> i = stateIterator(); i.hasNext(); ) {
			State state = i.next();
			checkCircularity(state, obligation, basicBlockId);
			state.getObligationSet().add(obligation);
			updatedStateMap.put(state.getObligationSet(), state);
			
//			if (state.getObligationSet().getCount(obligation.getId()) == 1) {
//				// This is the first addition of this kind of obligation.
//				// Make a note so we can use the information to report source
//				// line info.
//				state.getObligationSet().setWhereCreated(obligation, basicBlockId);
//			}
		}
		replaceMap(updatedStateMap);
	}

	/**
	 * Remove an Obligation from every State in the StateSet.
	 * 
	 * @param obligation the obligation to remove
	 * @param basicBlockId the id of the basic block (path component) removing the obligation
	 * @throws NonexistentObligationException
	 */
	public void deleteObligation(final Obligation obligation, int basicBlockId) throws ObligationAcquiredOrReleasedInLoopException {
		Map<ObligationSet, State> updatedStateMap = new HashMap<ObligationSet, State>();
		for (Iterator<State> i = stateIterator(); i.hasNext(); ) {
			State state = i.next();
			checkCircularity(state, obligation, basicBlockId);
			state.getObligationSet().remove(obligation);
			updatedStateMap.put(state.getObligationSet(), state);
		}
		replaceMap(updatedStateMap);
	}

	/**
	 * Bail out of the analysis is an obligation is
	 * acquired or released in a loop.
	 * 
	 * @param state a State to which an obligation is being added or removed
	 * @param obligation  the Obligation being added or removed
	 * @param basicBlockId  the id of the BasicBlock adding or removing the obligation
	 */
	private void checkCircularity(State state, Obligation obligation, int basicBlockId) throws ObligationAcquiredOrReleasedInLoopException {
		if (state.getPath().hasComponent(basicBlockId)) {
			throw new ObligationAcquiredOrReleasedInLoopException(obligation);
		}
	}
	
	/**
	 * Replace the map of ObligationSets to States with
	 * the given one.
	 * 
	 * @param stateMap enw map of ObligationSets to States
	 */
	public void replaceMap(Map<ObligationSet, State> stateMap) {
		this.stateMap = stateMap;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		StateSet other = (StateSet) o;
		return this.isTop == other.isTop
			&& this.isBottom == other.isBottom
			&& this.stateMap.equals(other.stateMap);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		if (isTop)
			return "TOP";
		else if (isBottom)
			return "BOTTOM";
		else {
			StringBuilder buf = new StringBuilder();
			boolean first = true;
			for (Iterator<State> i = stateIterator(); i.hasNext();) {
				State state = i.next();
				if (first)
					first = false;
				else
					buf.append(",");
				buf.append(state.toString());
			}
			return buf.toString();
		}
	}

	/**
	 * Return a newly allocated Map of ObligationSet to State
	 * that may be passed to applyToAllStatesAndUpdateMap().
	 */
	public Map<ObligationSet, State> createEmptyMap() {
		return new HashMap<ObligationSet, State>();
	}
}

// vim:ts=4
