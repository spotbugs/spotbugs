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

/**
 * Cached data for a source file.
 * Contains a map of line numbers to byte offsets, for quick
 * searching of source lines.
 * @see SourceFinder
 * @author David Hovemeyer
 */
public class SourceFile {
	private static final int DEFAULT_SIZE = 100;

	private byte[] data;
	private int[] lineNumberMap;
	private int numLines;

	/**
	 * Constructor.
	 * Creates an empty SourceFile object.
	 */
	public SourceFile() {
		this.lineNumberMap = new int[DEFAULT_SIZE];
		this.numLines = 0;
	}

	/**
	 * Set the source file data.
	 * @param data the data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Get the source file data.
	 * @param the data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Add a source line byte offset.
	 * This method should be called for each line in the source file,
	 * in order.
	 * @param offset the byte offset of the next source line
	 */
	public void addLineOffset(int offset) {
		if (numLines >= lineNumberMap.length) {
			// Grow the line number map.
			int capacity = lineNumberMap.length * 2;
			int[] newLineNumberMap = new int[capacity];
			System.arraycopy(lineNumberMap, 0, newLineNumberMap, 0, lineNumberMap.length);
			lineNumberMap = newLineNumberMap;
		}

		lineNumberMap[numLines++] = offset;
	}

	/**
	 * Get the byte offset in the data for a source line.
	 * @param line the line number
	 * @return the byte offset in the file's data for the line
	 * @throws IllegalArgumentException if the line number is out of bounds
	 */
	public int getLineOffset(int line) {
		if (line >= numLines) {
			throw new IllegalArgumentException("line is out of bounds (line=" + line +
				", numLines=" + numLines + ")");
		}
		return lineNumberMap[line];
	}
}

// vim:ts=4
