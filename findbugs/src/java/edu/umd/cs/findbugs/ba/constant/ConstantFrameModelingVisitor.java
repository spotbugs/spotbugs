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
package edu.umd.cs.findbugs.ba.constant;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;

import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;

/**
 * Visitor to model the effect of bytecode instructions
 * on ConstantFrames.
 * 
 * <p>For now, only String constants are modeled.
 * In the future we can add other kinds of constants.</p>
 * 
 * @see edu.umd.cs.findbugs.ba.constant.ConstantAnalysis
 * @author David Hovemeyer
 */
public class ConstantFrameModelingVisitor
		extends AbstractFrameModelingVisitor<Constant, ConstantFrame> {

	public ConstantFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}
			
	//@Override
	public Constant getDefaultValue() {
		return Constant.NOT_CONSTANT;
	}
	
	@Override
	public void visitLDC(LDC obj) {
		Object value = obj.getValue(getCPG());
		if (value instanceof String) {
			Constant c = new Constant(value);
			getFrame().pushValue(c);
		} else {
			// FIXME: other kinds of constants
			super.visitLDC(obj);
		}
	}
	
	@Override
	public void visitLDC2_W(LDC2_W obj) {
		Object value = obj.getValue(getCPG());
		if (value instanceof String) {
			Constant c = new Constant(value);
			getFrame().pushValue(c);
		} else {
			// FIXME: other kinds of constants
			super.visitLDC2_W(obj);
		}
	}

}
