/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import org.apache.bcel.generic.InstructionHandle;
import java.util.*;

/**
 * Abstract base class providing functionality that will be useful
 * for most dataflow analysis implementations.  In particular, it implements
 * the transfer() function by calling down to the transferInstruction() function.
 * It also maintains a map of the dataflow fact for every location in the CFG,
 * which is useful when using the results of the analysis.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @author David Hovemeyer
 */
public abstract class AbstractDataflowAnalysis<Fact> implements DataflowAnalysis<Fact> {
	private static final boolean DEBUG = Boolean.getBoolean("dataflow.transfer");

	private HashMap<Location, Fact> factAtLocationMap = new HashMap<Location, Fact>();
	private HashMap<Location, Fact> factAfterLocationMap = new HashMap<Location, Fact>();

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Transfer function for a single instruction.
	 * @param handle the instruction
	 * @param basicBlock the BasicBlock containing the instruction; needed to disambiguate
	 *  instructions in inlined JSR subroutines
	 * @param fact which should be modified based on the instruction
	 */
	public abstract void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, Fact fact) throws DataflowAnalysisException;

	/**
	 * Determine whether the given fact is <em>valid</em>
	 * (neither top nor bottom).
	 */
	public abstract boolean isFactValid(Fact fact);

	/**
	 * Get the dataflow fact representing the point just before given Location.
	 * Note "before" is meant in the logical sense, so for backward analyses,
	 * before means after the location in the control flow sense.
	 * @param location the location
	 * @return the fact at the point just before the location
	 */
	public Fact getFactAtLocation(Location location) {
		Fact fact = factAtLocationMap.get(location);
		if (fact == null) {
			fact = createFact();
			factAtLocationMap.put(location, fact);
		}
		return fact;
	}

	/**
	 * Get the dataflow fact representing the point just after given Location.
	 * Note "after" is meant in the logical sense, so for backward analyses,
	 * after means before the location in the control flow sense.
	 */
	public Fact getFactAfterLocation(Location location) {
		Fact fact = factAfterLocationMap.get(location);
		if (fact == null) {
			fact = createFact();
			factAfterLocationMap.put(location, fact);
		}
		return fact;
	}

	/**
	 * Get an Iterator over all dataflow facts that we've recorded for
	 * the Locations in the CFG.
	 */
	public Iterator<Fact> factIterator() {
		return factAtLocationMap.values().iterator();
	}

	/* ----------------------------------------------------------------------
	 * Implementations of interface methods
	 * ---------------------------------------------------------------------- */

	public void transfer(BasicBlock basicBlock, InstructionHandle end, Fact start, Fact result) throws DataflowAnalysisException {
		copy(start, result);

		if (isFactValid(result)) {
			Iterator<InstructionHandle> i = isForwards() ? basicBlock.instructionIterator() : basicBlock.instructionReverseIterator();

			Location prevLocation = null;

			while (i.hasNext()) {
				InstructionHandle handle = i.next();
				if (handle == end)
					break;
	
				// Record the fact at this location
				Location location = new Location(handle, basicBlock);
				Fact factAtLocation = getFactAtLocation(location);
				copy(result, factAtLocation);

				// The fact AT this location is also the fact AFTER the
				// previous location.
				if (end == null && prevLocation != null)
					factAfterLocationMap.put(prevLocation, factAtLocation);
				prevLocation = location;

				if (DEBUG) System.out.print("Transfer " + result.toString() + " for " + handle);
	
				// Transfer the dataflow value
				transferInstruction(handle, basicBlock, result);

				if (DEBUG) System.out.println(" ==> " + result.toString());
			}

			if (end == null && prevLocation != null) {
				// The fact AFTER the last location is the result fact.
				Location lastLocation = prevLocation;
				Fact factAfterLocation = getFactAfterLocation(lastLocation);
				copy(result, factAfterLocation);
			}
		}
	}

}

// vim:ts=4
