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

/**
 * Instruction in a register-based bytecode intermediate representation.
 * Represents a bytecode instruction, destination register, and source
 * register(s). 
 * 
 * <p>Note: this class is just an idea sketch.  There is no guarantee
 * it will ever be used for anything.</p>
 * 
 * @author David Hovemeyer
 */
public class Quad {
	private Instruction instruction;
	private short dest;
	private short r1, r2, r3;
	
	public Quad(Instruction instruction) {
		this.instruction = instruction;
	}
	
	public Quad(Instruction instruction, short dest) {
		this.instruction = instruction;
		this.dest = dest;
	}
	
	public Quad(Instruction instruction, short dest, short r1) {
		this.instruction = instruction;
		this.dest = dest;
		this.r1 = r1;
	}
	
	public Quad(Instruction instruction, short dest, short r1, short r2) {
		this.instruction = instruction;
		this.dest = dest;
		this.r1 = r1;
		this.r2 = r2;
	}
	
	public Quad(Instruction instruction, short dest, short r1, short r2, short r3) {
		this.instruction = instruction;
		this.dest = dest;
		this.r1 = r1;
		this.r2 = r2;
		this.r3 = r3;
	}
	
	public Instruction getInstruction() {
		return instruction;
	}
	
	public short getDest() {
		return dest;
	}
	
	public short getR1() {
		return r1;
	}
	
	public short getR2() {
		return r2;
	}
	
	public short getR3() {
		return r3;
	}
}
