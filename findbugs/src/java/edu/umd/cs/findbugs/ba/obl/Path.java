/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.obl;

/**
 * A Path is a sequence of program statements.
 * For our purposes, basic blocks are considered statements.
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 *
 * @author David Hovemeyer
 */
public class Path {
	private static final int DEFAULT_CAPACITY = 8;

	private int[] blockIdList;
	private int length;

	public Path() {
		this.blockIdList = new int[DEFAULT_CAPACITY];
		this.length = 0;
	}

	public void append(int id) {
		grow(length);
		blockIdList[length] = id;
		++length;
	}

	public int getBlockIdAt(int index) {
		return blockIdList[index];
	}

	public int getLength() {
		return length;
	}
	
	public Path duplicate() {
		Path dup = new Path();
		dup.grow(this.length - 1);
		
		dup.length = this.length;
		for (int i = 0; i < this.length; ++i) {
			dup.blockIdList[i] = this.blockIdList[i];
		}
		
		return dup;
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
