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
package edu.umd.cs.findbugs.ba.ir;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.Visitor;

/**
 * "Extended" Instruction class for synthetic instructions
 * used in the register-based IR.  The only additional functionality is being
 * able to accept an ExtendedVisitor.
 * 
 * <p>Note: this class is just an idea sketch.  There is no guarantee
 * it will ever be used for anything.</p>
 *  
 * @author David Hovemeyer
 */
public abstract class ExtendedInstruction extends Instruction {

	public ExtendedInstruction(short opcode, short len) {
		super(opcode, len);
	}
	
	@Override
         public int hashCode() {
		throw new UnsupportedOperationException("hashCode not supported on Instructions");
	}
	@Override
         public void accept(Visitor visitor) {
		if (visitor instanceof ExtendedVisitor) {
			accept((ExtendedVisitor) visitor);
		}
	}
	
	public abstract void accept(ExtendedVisitor visitor);
}
