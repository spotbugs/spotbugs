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

import org.apache.bcel.generic.*;

public class IsNullValueFrameModelingVisitor extends AbstractFrameModelingVisitor<IsNullValue> {

	public IsNullValueFrameModelingVisitor(IsNullValueFrame frame, ConstantPoolGen cpg) {
		super(frame, cpg);
	}

	public IsNullValue getDefaultValue() {
		return IsNullValue.notDefinitelyNull();
	}

	// Overrides of specific instruction visitor methods.
	// ACONST_NULL obviously produces a value that is DEFINITELY NULL.
	// If a reference value is dereferenced (by a load, store, or
	// method invocation), then afterwards it is NOT NULL.
	// LDC produces values that are NOT NULL.
	// NEW produces values that are NOT NULL.

	// Note that we don't override IFNULL and IFNONNULL.
	// Those are handled in the analysis itself, because we need
	// to produce different values in each of the control successors.

	public void visitACONST_NULL(ACONST_NULL obj) {
		Frame<IsNullValue> frame = getFrame();
		frame.pushValue(IsNullValue.definitelyNull());
	}

	public void visitPUTFIELD(PUTFIELD obj) {
	}

}

// vim:ts=4
