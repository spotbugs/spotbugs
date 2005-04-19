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
 * Opcodes for "extended" synthetic instructions used in the
 * register-based IR.  These won't appear in any actual
 * classfile.
 * 
 * <p>Note: this class is just an idea sketch.  There is no guarantee
 * it will ever be used for anything.</p>
 * 
 * @author David Hovemeyer
 */
public interface ExtendedOpcodes {
	
	// Opcodes in the ranges 203-219 and 221-228 are reserved
	// as the "quick" opcodes, and won't appear in any actual classfile.
	
	/**
	 * Null check.
	 */
	public static final short NULLCHECK = (short) 203;
	
	/**
	 * Clear operand stack and push caught exception object.
	 */
	public static final short PUSHEXCEPTION = (short) 204;
}
