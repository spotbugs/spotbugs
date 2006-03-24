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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A "meta" PatternElement that matches any of a list of other child PatternElements.
 * An example of how this is useful is that you might want to match invocations of any
 * of a number of different methods.  To do this, you can create a MatchAny
 * with some number of Invoke elements as children.
 * <p/>
 * <p> Note that the minOccur() and maxOccur() counts of the child PatternElements
 * are ignored.  A MatchAny element always matches exactly one instruction.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class MatchAny extends PatternElement {
	private PatternElement[] childList;

	/**
	 * Constructor.
	 *
	 * @param childList list of child PatternElements
	 */
	@SuppressWarnings("EI2")
	public MatchAny(PatternElement[] childList) {
		this.childList = childList;
	}

	@Override
         public PatternElement label(String label) {
		for (PatternElement aChildList : childList) {
			aChildList.label(label);
		}
		return this;
	}

	@Override
         public PatternElement setAllowTrailingEdges(boolean allowTrailingEdges) {
		// Just forward this on to all children,
		// since it is the children that the PatternMatcher will ask
		// about edges.
		for (PatternElement aChildList : childList)
			aChildList.setAllowTrailingEdges(allowTrailingEdges);

		return this;
	}

	@Override
         public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
	                         ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		for (PatternElement child : childList) {
			MatchResult matchResult = child.match(handle, cpg, before, after, bindingSet);
			if (matchResult != null)
				return matchResult;
		}

		return null;

	}

	@Override
         public boolean acceptBranch(Edge edge, InstructionHandle source) {
		// Note: when selecting branch instructions, only the actual
		// (child) PatternElement should be used.
		throw new IllegalStateException("shouldn't happen");
	}

	@Override
         public int minOccur() {
		return 1;
	}

	@Override
         public int maxOccur() {
		return 1;
	}
}

// vim:ts=4
