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

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A class representing a location in the CFG for a method.
 * Essentially, it represents a static instruction, <em>with the important caveat</em>
 * that CFGs have inlined JSR subroutines, meaning that a single InstructionHandle
 * in a CFG may represent several static locations.  To this end, a Location
 * is comprised of both an InstructionHandle and the BasicBlock that
 * contains it.
 * <p/>
 * <p> Location objects may be compared with each other using the equals() method,
 * and may be used as keys in tree and hash maps and sets.
 * Note that <em>it is only valid to compare Locations produced from the same CFG</em>.
 *
 * @author David Hovemeyer
 * @see CFG
 */
public class Location implements Comparable<Location> {
	private final InstructionHandle handle;
	private final BasicBlock basicBlock;

	/**
	 * Constructor.
	 *
	 * @param handle     the instruction
	 * @param basicBlock the basic block containing the instruction
	 */
	public Location(@NonNull InstructionHandle handle, @NonNull BasicBlock basicBlock) {
		if (handle == null) throw new NullPointerException("handle cannot be null");
		if (basicBlock == null) throw new NullPointerException("basicBlock cannot be null");
		this.handle = handle;
		this.basicBlock = basicBlock;
	}

	public static Location getFirstLocation(@NonNull BasicBlock basicBlock) {
		InstructionHandle location = basicBlock.getFirstInstruction();
		if (location == null)
			return null;
		return new Location(location, basicBlock);
	}
	public static Location getLastLocation(@NonNull BasicBlock basicBlock) {
		InstructionHandle lastInstruction = basicBlock.getLastInstruction();
		/*
		if (lastInstruction == null)
			lastInstruction = basicBlock.getExceptionThrower();
		if (lastInstruction == null)
			lastInstruction = basicBlock.getFirstInstruction();
		*/
		if (lastInstruction == null)
			return null;
		return new Location(lastInstruction, basicBlock);
	}
	/**
	 * Get the instruction handle.
	 */
	public InstructionHandle getHandle() {
		return handle;
	}

	/**
	 * Get the basic block.
	 */
	public BasicBlock getBasicBlock() {
		return basicBlock;
	}

	/**
	 * Return whether or not the Location is positioned at the
	 * first instruction in the basic block.
	 */
	public boolean isFirstInstructionInBasicBlock() {
		return !basicBlock.isEmpty() && handle == basicBlock.getFirstInstruction();
	}

	/**
	 * Return whether or not the Location is positioned at the
	 * last instruction in the basic block.
	 */
	public boolean isLastInstructionInBasicBlock() {
		return !basicBlock.isEmpty() && handle == basicBlock.getLastInstruction();
	}

	public int compareTo(Location other) {
		int cmp = basicBlock.getId() - other.basicBlock.getId();
		if (false && cmp != 0)
			return cmp;

		int pos = handle.getPosition() - other.handle.getPosition();
		if (pos != 0) return pos;
		return cmp;
	}

	@Override
		 public int hashCode() {
		return System.identityHashCode(basicBlock) + handle.getPosition();
	}

	@Override
		 public boolean equals(Object o) {
		if (!(o instanceof Location))
			return false;
		Location other = (Location) o;
		return basicBlock == other.basicBlock && handle == other.handle;
	}

	@Override
		 public String toString() {
		return handle.toString() + " in basic block " + basicBlock.getId();
	}
}

// vim:ts=4
