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

import org.apache.bcel.*;
import org.apache.bcel.generic.*;

/**
 * Model the effects of Java bytecode instructions in order to
 * analyze which stack slots definitely contain the "this" reference.
 *
 * @see ThisValueFrame
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class ThisValueFrameModelingVisitor extends AbstractFrameModelingVisitor<ThisValue> {
	/**
	 * Constructor.
	 * @param frame the frame to be transformed
	 * @param cpg the ConstantPoolGen of the method to be analyzed
	 */
	public ThisValueFrameModelingVisitor(ThisValueFrame frame, ConstantPoolGen cpg) {
		super(frame, cpg);
	}

	/**
	 * Produce the default value.
	 * We assume that any instruction which doesn't just copy a value
	 * from another stack slot will only produce "non-this" values.
	 */
	public ThisValue getDefaultValue() {
		return ThisValue.notThisValue();
	}
}

// vim:ts=4
