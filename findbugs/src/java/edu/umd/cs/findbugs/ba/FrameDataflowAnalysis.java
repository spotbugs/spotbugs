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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A convenient base class for dataflow analysis classes which
 * use Frames as values.
 *
 * @author David Hovemeyer
 * @see Frame
 * @see DataflowAnalysis
 */
public abstract class FrameDataflowAnalysis <ValueType, FrameType extends Frame<ValueType>> extends ForwardDataflowAnalysis<FrameType> {
	public FrameDataflowAnalysis(DepthFirstSearch dfs) {
		super(dfs);
	}

	public void copy(FrameType source, FrameType dest) {
		dest.copyFrom(source);
	}

	public void makeFactTop(FrameType fact) {
		fact.setTop();
	}
	public boolean isTop(FrameType fact) {
		return fact.isTop();
	}
	public boolean same(FrameType fact1, FrameType fact2) {
		return fact1.sameAs(fact2);
	}

	@Override
		 public boolean isFactValid(FrameType fact) {
		return fact.isValid();
	}

	@Override
	public int getLastUpdateTimestamp(FrameType fact) {
		return fact.getLastUpdateTimestamp();
	}
	@Override
	public void setLastUpdateTimestamp(FrameType fact, int lastTimestamp) {
		fact.setLastUpdateTimestamp(lastTimestamp);
	}

	/**
	 * Create a modifiable copy of a frame.
	 * This is useful for meetInto(), if the frame needs to be
	 * modified in a path-sensitive fashion.
	 * A typical usage pattern is:
	 * <p/>
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
	 * <p/>
	 * mergeInto(fact, result);
	 * </pre>
	 * <p/>
	 * The advantage of using modifyFrame() is that new code can be added
	 * before or after other places where the frame is modified, and the
	 * code will remain correct.
	 *
	 * @param orig the original frame
	 * @param copy the modifiable copy (returned by a previous call to modifyFrame()),
	 *             or null if this is the first time modifyFrame() is being called
	 * @return a modifiable copy of fact
	 */
	final protected FrameType modifyFrame(FrameType orig, @Nullable FrameType copy) {
		if (copy == null) {
			copy = createFact();
			copy.copyFrom(orig);
		}
		return copy;
	}

	/**
	 * Merge one frame into another.
	 *
	 * @param other  the frame to merge with the result
	 * @param result the result frame, which is modified to be the
	 *               merge of the two frames
	 */
	protected void mergeInto(FrameType other, FrameType result) throws DataflowAnalysisException {
		// Handle if result Frame or the other Frame is the special "TOP" value.
		if (result.isTop()) {
			// Result is the identity element, so copy the other Frame
			result.copyFrom(other);
			return;
		} else if (other.isTop()) {
			// Other Frame is the identity element, so result stays the same
			return;
		}

		// Handle if result Frame or the other Frame is the special "BOTTOM" value.
		if (result.isBottom()) {
			// Result is the bottom element, so it stays that way
			return;
		} else if (other.isBottom()) {
			// Other Frame is the bottom element, so result becomes the bottom element too
			result.setBottom();
			return;
		}

		// If the number of slots in the Frames differs,
		// then the result is the special "BOTTOM" value.
		if (result.getNumSlots() != other.getNumSlots()) {
			result.setBottom();
			return;
		}

		// Usual case: ordinary Frames consisting of the same number of values.
		// Merge each value in the two slot lists element-wise.
		for (int i = 0; i < result.getNumSlots(); ++i) {
			mergeValues(other, result, i);
		}
	}

	/**
	 * Merge the values contained in a given slot of two Frames.
	 * 
	 * @param otherFrame  a Frame
	 * @param resultFrame a Frame which will contain the resulting merged value
	 * @param slot        a slot in both frames
	 * @throws DataflowAnalysisException
	 */
	protected abstract void mergeValues(FrameType otherFrame, FrameType resultFrame, int slot)
			throws DataflowAnalysisException;
}

// vim:ts=4
