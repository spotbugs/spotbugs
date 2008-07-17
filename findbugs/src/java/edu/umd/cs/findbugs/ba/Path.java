/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004,2008 University of Maryland
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

/**
 * A Path is a sequence of basic blocks.
 *
 * @author David Hovemeyer
 */
public class Path {
	private static final int DEFAULT_CAPACITY = 8;
	private static final int INVALID_HASH_CODE = -1;

	private int[] blockIdList;
	private int length;
	private int cachedHashCode;

	/**
	 * Constructor.
	 * Creates an empty Path.
	 */
	public Path() {
		this.blockIdList = new int[DEFAULT_CAPACITY];
		this.length = 0;
		invalidate();
	}

	/**
	 * Append given BasicBlock id to the path.
	 * 
	 * @param id a BasicBlock id (label)
	 */
	public void append(int id) {
		grow(length);
		blockIdList[length] = id;
		++length;
		invalidate();
	}
	
	/**
	 * Determine whether or not the id of the given BasicBlock appears
	 * anywhere in the path.
	 * 
	 * @param blockId the id (label) of a BasicBlock
	 * @return true if the BasicBlock's id appears in the path, false if not
	 */
	public boolean hasComponent(int blockId) {
		for (int i = 0; i < length; i++) {
			if (blockIdList[i] == blockId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the BasicBlock id at the given index in the path.
	 * 
	 * @param index an index in the Path (0 is the first component)
	 * @return the id of the BasicBlock at the given index
	 */
	public int getBlockIdAt(int index) {
		assert index < length;
		return blockIdList[index];
	}

	/**
	 * Get the number of components (BasicBlock ids) in the Path.
	 * 
	 * @return number of components in the Path
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Return an exact copy of this Path.
	 * 
	 * @return an exact copy of this Path
	 */
	public Path duplicate() {
		Path dup = new Path();
		dup.copyFrom(this);
		return dup;
	}

	/**
	 * Make this Path identical to the given one.
	 * 
	 * @param other a Path to which this object should be made identical
	 */
	public void copyFrom(Path other) {
		grow(other.length - 1);
		System.arraycopy(other.blockIdList, 0, this.blockIdList, 0, other.length);
		this.length = other.length;
		this.cachedHashCode = other.cachedHashCode;
	}

	private void invalidate() {
		this.cachedHashCode = INVALID_HASH_CODE;
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == INVALID_HASH_CODE) {
			int value = 0;
			for (int i = 0; i < this.length; ++i) {
				value += (i * 1009 * blockIdList[i]);
			}
			cachedHashCode = value;
		}
		return cachedHashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		Path other = (Path) o;
		if (this.length != other.length)
			return false;
		for (int i = 0; i < this.length; ++i) {
			if (this.blockIdList[i] != other.blockIdList[i])
				return false;
		}
		return true;
	}

	private static final String SYMBOLS =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()";

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			int block = blockIdList[i];
			if (block < SYMBOLS.length())
				buf.append(SYMBOLS.charAt(block));
			else
				buf.append("'" + block + "'");
		}
		return buf.toString();
	}

	private void grow(int index) {
		if (index >= blockIdList.length) {
			int newLen = blockIdList.length;
			do {
				newLen *= 2;
			} while (index >= newLen);

			int[] arr = new int[newLen];
			System.arraycopy(this.blockIdList, 0, arr, 0, length);
			this.blockIdList = arr;
		}
	}
}

// vim:ts=4
