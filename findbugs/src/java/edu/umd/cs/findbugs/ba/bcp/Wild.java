/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba.bcp;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A wildcard PatternElement, which matches any kind of instruction
 * indiscriminately.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class Wild extends PatternElement {
	private int min, max;

	/**
	 * Default constructor.
	 * Creates a wildcard that matches from 0 to Integer.MAX_VALUE instructions.
	 */
	public Wild() {
		this.min = 0;
		this.max = Integer.MAX_VALUE;
	}

	/**
	 * Constructor.  Matches any number of instructions from 0 to the maximum specified.
	 *
	 * @param max the maximum number of instructions the wildcard may match
	 */
	public Wild(int max) {
		this.min = 0;
		this.max = max;
	}

	/**
	 * Constructor.
	 *
	 * @param min minimum number of times the wildcard must match
	 * @param max maximum number of times the wildcard may match
	 */
	public Wild(int min, int max) {
		this.min = min;
		this.max = max;
	}

	/**
	 * Set min and max values.
	 *
	 * @param min minimum number of times the wildcard must match
	 * @param max maximum number of times the wildcard may match
	 */
	public void setMinAndMax(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
         public int minOccur() {
		return min;
	}

	@Override
         public int maxOccur() {
		return max;
	}

	@Override
         public boolean acceptBranch(Edge edge, InstructionHandle source) {
		return true;
	}

	@Override
         public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
	                         ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {
		return new MatchResult(this, bindingSet);
	}
}

// vim:ts=4
