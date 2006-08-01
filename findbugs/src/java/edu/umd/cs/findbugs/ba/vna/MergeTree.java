/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.ba.vna;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Data structure to keep track of which input ValueNumbers were
 * combined to produce which other output ValueNumbers. 
 * 
 * @author David Hovemeyer
 */
public class MergeTree {
	public static final boolean DEBUG = SystemProperties.getBoolean("vna.merge.debug");
	
	private ValueNumberFactory factory;
	private Map<ValueNumber, BitSet> outputToInputMap;
	
	/**
	 * Constructor.
	 * 
	 * @param factory the ValueNumberFactory
	 */
	public MergeTree(ValueNumberFactory factory) {
		this.factory = factory;
		this.outputToInputMap = new HashMap<ValueNumber, BitSet>();
	}
	
	/**
	 * Map an input ValueNumber to an output ValueNumber.
	 * 
	 * @param input  the input ValueNumber
	 * @param output the output ValueNumber
	 */
	public void mapInputToOutput(ValueNumber input, ValueNumber output) {
		BitSet inputSet = getInputSet(output);
		inputSet.set(input.getNumber());
		if (DEBUG) {
			System.out.println(input.getNumber()+ "->" + output.getNumber());
			System.out.println("Input set for " + output.getNumber() + " is now " + inputSet);
		}
	}

	/**
	 * Get the set of input ValueNumbers which directly contributed to
	 * the given output ValueNumber.
	 * 
	 * @param output the output ValueNumber
	 * @return the set of direct input ValueNumbers
	 */
	public BitSet getInputSet(ValueNumber output) {
		BitSet outputSet = outputToInputMap.get(output);
		if (outputSet == null) {
			if (DEBUG) {
				System.out.println("Create new input set for " + output.getNumber());
			}
			outputSet = new BitSet();
			outputToInputMap.put(output, outputSet);
		}
		return outputSet;
	}
	
	/**
	 * Get the transitive set of input ValueNumbers which contributed
	 * (directly or indirectly) to the given output ValueNumber.
	 * 
	 * @param output the output ValueNumber
	 * @return the transitive set of input ValueNumbers
	 */
	public BitSet getTransitiveInputSet(ValueNumber output) {
		BitSet visited = new BitSet();
		BitSet result = new BitSet();
		
		if (DEBUG) {
			System.out.println("Output: " + output.getNumber());
		}
		
		LinkedList<ValueNumber> workList = new LinkedList<ValueNumber>();
		workList.addLast(output);
		while (!workList.isEmpty()) {
			ValueNumber valueNumber = workList.removeFirst();
			if (DEBUG) {
				System.out.println("Check: " + valueNumber.getNumber());
			}
			visited.set(valueNumber.getNumber());
			BitSet inputSet = getInputSet(valueNumber);
			if (DEBUG) {
				System.out.println("\tInput set is " + inputSet);
			}
			result.or(inputSet);
			for (int i = 0; i < factory.getNumValuesAllocated(); ++i) {
				if (inputSet.get(i) && !visited.get(i)) {
					if (DEBUG) {
						System.out.println("\tExplore: " + i);
					}
					workList.addLast(factory.forNumber(i));
				}
			}
		}
		if (DEBUG) {
			System.out.println("Result: " + result);
		}
		
		return result;
	}
}
