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

/**
 * Synthetic instruction to clear the operand stack and push
 * the caught exception object.  This will appear as the first
 * instruction in every catch block.
 * 
 * <p>Note: this class is just an idea sketch.  There is no guarantee
 * it will ever be used for anything.</p>
 * 
 * @author David Hovemeyer
 * @deprecated
 */
public class PushException extends ExtendedInstruction {
	private static final long serialVersionUID = 1;
	
	private static final PushException instance = new PushException();
	
	public static PushException instance() { return instance; }
	
	private PushException() {
		super(ExtendedOpcodes.PUSHEXCEPTION, (short) 1);
	}

	//@Override
	@Override
         public void accept(ExtendedVisitor visitor) {
		visitor.visitPushException(this);
	}

}
