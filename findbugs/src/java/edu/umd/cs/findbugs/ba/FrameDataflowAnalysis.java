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
		return fact1.sameAs(fact1);
	}

	public boolean isFactValid(FrameType fact) {
		return fact.isValid();
	}
}

// vim:ts=4
