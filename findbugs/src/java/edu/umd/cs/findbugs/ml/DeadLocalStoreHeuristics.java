/*
 * FindBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs.ml;

/**
 * Bug properties for encoring dead local store heuristics.
 * 
 * @see edu.umd.cs.findbugs.BugProperty
 * @see edu.umd.cs.findbugs.detect.FindDeadLocalStores
 * @author David Hovemeyer
 */
public interface DeadLocalStoreHeuristics {
	// Data that may be of interest to false-positive-reducing heuristics
	
	/** Name of local variable. */
	public static final String LOCAL_NAME = "dls.localName";
	
	/** Store was killed by a subsequent store. */
	public static final String KILLED_BY_SUBSEQUENT_STORE = "dls.killedBySubsequentStore";
	
	/** Defensive constant opcode seen. */
	public static final String DEFENSIVE_CONSTANT_OPCODE = "dls.defensiveConstant";
	
	/** Exception handler. */
	public static final String EXCEPTION_HANDLER = "dls.exceptionHandler";
	
	/** Store is a dead increment. */
	public static final String DEAD_INCREMENT ="dls.deadIncrement";
	
	/** Method contains a single increment, which is not used. */
	public static final String SINGLE_DEAD_INCREMENT = "dls.singleDeadIncrement";
	
	/** Dead store is of a newly allocated object or array. */
	public static final String DEAD_OBJECT_STORE = "dls.deadObjectStore";
	
	/** Two stores and muliple loads of a local (where one of the stores is dead). */
	public static final String TWO_STORES_MULTIPLE_LOADS = "dls.twoStoresMultipleLoads";
	
	/** Only a single store of the local. */
	public static final String SINGLE_STORE = "dls.singleStore";
	
	/** No loads of the dead store. */
	public static final String NO_LOADS = "dls.noLoads";

}
