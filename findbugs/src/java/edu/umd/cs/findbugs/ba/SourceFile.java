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

import java.io.*;

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
	 * Get an InputStream on data.
	 * @return an InputStream on the data in the source file,
	 *   starting from given offset
	 */
	public InputStream getInputStream() {
		return new ByteArrayInputStream(data);
	}

	/**
	 * Get an InputStream on data starting at given offset.
	 * @param offset the start offset
	 * @return an InputStream on the data in the source file,
	 *   starting at the given offset
	 */
	public InputStream getInputStreamFromOffset(int offset) {
		return new ByteArrayInputStream(data, offset, data.length - offset);
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
	 * Note that lines are considered to be zero-index, so the first
	 * line in the file is numbered zero.
	 * @param line the line number
	 * @return the byte offset in the file's data for the line,
	 *   or -1 if the line is not valid
	 */
	public int getLineOffset(int line) {
		if (line < 0 || line >= numLines)
			return -1;
		return lineNumberMap[line];
	}
}

// vim:ts=4
