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

public class ValueNumberFrame extends Frame<ValueNumber> {

	private ValueNumberFactory factory;
	private int nextValueNumber;

	public ValueNumberFrame(int numLocals, ValueNumberFactory factory) {
		super(numLocals);
		this.factory = factory;
		nextValueNumber = 0;
	}

	public void setNextValueNumber(int nextValueNumber) {
		this.nextValueNumber = nextValueNumber;
	}

	public ValueNumber mergeValues(ValueNumber a, ValueNumber b) {
		return a.mergeWith(b);
	}

	public ValueNumber getDefaultValue() {
		return factory.topValue();
	}

	public void copyFrom(Frame<ValueNumber> other_) {
		ValueNumberFrame other = (ValueNumberFrame) other_;
		nextValueNumber = other.nextValueNumber;
		super.copyFrom(other);
	}

	public void mergeWith(Frame<ValueNumber> other_) throws DataflowAnalysisException {
		ValueNumberFrame other = (ValueNumberFrame) other_;
		nextValueNumber = Math.max(nextValueNumber, other.nextValueNumber);
		super.mergeWith(other);
	}

}

// vim:ts=4
