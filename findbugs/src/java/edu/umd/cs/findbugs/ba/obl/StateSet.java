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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	public interface StateCallback {
		public void apply(State state) throws NonexistentObligationException;
	}
	
	private boolean isTop;
	private boolean isBottom;
	private Map<ObligationSet, State> stateMap;

	public StateSet() {
		this.isTop = this.isBottom = false;
		this.stateMap = new HashMap<ObligationSet, State>();
	}

	public void setTop() {
		this.isTop = true;
		this.isBottom = false;
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
	 * Get the State which has the given ObligationSet.
	 * Returns null if there is no such state.
	 * 
	 * @param obligationSet we want to get the State with this ObligationSet
	 * @return the State with the given ObligationSet, or null if there is no such State
	 */
	public State getStateWithObligationSet(ObligationSet obligationSet) {
		return stateMap.get(obligationSet);
	}
	
	/**
	 * Initialize this object as the entry fact for a method:
	 * a single state with empty obligation set and path.
	 * 
	 * @param factory the ObligationFactory used for the analysis
	 */
	public void initEntryFact(ObligationFactory factory) {
		this.isTop = this.isBottom = false;
		this.stateMap.clear();
		
		// Add initial fact: empty obligations, empty path
		State initState = new State(factory.getMaxObligationTypes());
		this.stateMap.put(initState.getObligationSet(), initState);
	}
	
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
		StateSet dup = new StateSet();
		dup.copyFrom(this);
		return dup;
	}
	
	/**
	 * Add an obligation to every State in the StateSet.
	 * 
	 * @param obligation the obligation to add
	 */
	public void addObligation(final Obligation obligation) {
		final Map<ObligationSet, State> updatedStateMap =
			new HashMap<ObligationSet, State>();
		
		try {
			applyToAllStatesAndUpdateMap(new StateCallback() {
				public void apply(State state) {
					state.getObligationSet().add(obligation);
					updatedStateMap.put(state.getObligationSet(), state);
				}
			}, updatedStateMap);
		} catch (NonexistentObligationException e) {
			// This can't actually happen.
		}
	}
	
	/**
	 * Remove an Obligation from every State in the StateSet.
	 * 
	 * @param obligation the obligation to remove
	 * @throws NonexistentObligationException
	 */
	public void deleteObligation(final Obligation obligation)
			throws NonexistentObligationException {
		final Map<ObligationSet, State> updatedStateMap =
			new HashMap<ObligationSet, State>();
		
		applyToAllStatesAndUpdateMap(new StateCallback() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.obl.StateSet.StateCallback#apply(edu.umd.cs.findbugs.ba.obl.State)
			 */
			public void apply(State state)
					throws NonexistentObligationException {
				state.getObligationSet().remove(obligation);
				updatedStateMap.put(state.getObligationSet(), state);
			}
		}, updatedStateMap);
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
			StringBuffer buf = new StringBuffer();
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
	
	/**
	 * Apply a callback to all States and replace the
	 * ObligationSet -&gt; State map with the one given
	 * (which is assumed to be updated by the callback.)
	 * 
	 * @param callback        the callback
	 * @param updatedStateMap updated map of ObligationSets to States
	 */
	public void applyToAllStatesAndUpdateMap(StateCallback callback,
			Map<ObligationSet, State> updatedStateMap)
			throws NonexistentObligationException {
		applyToAllStates(callback);
		this.stateMap = updatedStateMap;
	}

	/**
	 * Apply a callback to all States in the StateSet.
	 * 
	 * @param callback
	 */
	public void applyToAllStates(StateCallback callback)
			throws NonexistentObligationException {
		for (State state : stateMap.values()) {
			callback.apply(state);
		}
	}
}

// vim:ts=4
