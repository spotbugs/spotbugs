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

/**
 * A convenient base class for dataflow analysis classes which
 * use Frames as values.
 * @see Frame
 * @see DataflowAnalysis
 * @author David Hovemeyer
 */
public abstract class FrameDataflowAnalysis<ValueType, FrameType extends Frame<ValueType>> extends ForwardDataflowAnalysis<FrameType> {
	public FrameDataflowAnalysis(DepthFirstSearch dfs) {
		super(dfs);
	}

	public void copy(FrameType source, FrameType dest) {
		dest.copyFrom(source);
	}

	public void initResultFact(FrameType result) {
		result.setTop();
	}

	public void makeFactTop(FrameType fact) {
		fact.setTop();
	}

	public boolean same(FrameType fact1, FrameType fact2) {
		return fact1.sameAs(fact2);
	}

	public boolean isFactValid(FrameType fact) {
		return fact.isValid();
	}

	/**
	 * Create a modifiable copy of a frame.
	 * This is useful for meetInto(), if the frame needs to be
	 * modified in a path-sensitive fashion.
	 * A typical usage pattern is:
	 *
	 * <pre>
	 * FrameType copy = null;
	 * if (someCondition()) {
	 *     copy = modifyFrame(fact, copy);
	 *     // modify copy
	 * }
     * if (someOtherCondition()) {
	 *     copy = modifyFrame(fact, copy);
	 *     // modify copy
	 * }
	 * if (copy != null)
	 *     fact = copy;
	 *
	 * result.mergeWith(fact);
	 * </pre>
	 *
	 * The advantage of using modifyFrame() is that new code can be added
	 * before or after other places where the frame is modified, and the
	 * code will remain correct.
	 *
	 * @param orig the original frame
	 * @param copy the modifiable copy (returned by a previous call to modifyFrame()),
	 *   or null if this is the first time modifyFrame() is being called
	 * @param a modifiable copy of fact
	 */
	protected FrameType modifyFrame(FrameType orig, FrameType copy) {
		if (copy == null) {
			copy = createFact();
			copy.copyFrom(orig);
		}
		return copy;
	}
}

// vim:ts=4
