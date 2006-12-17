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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cached data for a source file.
 * Contains a map of line numbers to byte offsets, for quick
 * searching of source lines.
 *
 * @author David Hovemeyer
 * @see SourceFinder
 */
public class SourceFile {
	private static int intValueOf(byte b) {
		return b & 0xff;
	}

	/**
	 * Helper object to build map of line number to byte offset
	 * for a source file.
	 */
	private static class LineNumberMapBuilder {
		private SourceFile sourceFile;
		private int offset;
		private int lastSeen;

		public LineNumberMapBuilder(SourceFile sourceFile) {
			this.sourceFile = sourceFile;
			this.offset = 0;
			this.lastSeen = -1;
		}

		public void addData(byte[] data, int len) {
			for (int i = 0; i < len; ++i) {
				int ch = intValueOf(data[i]);
				//if (ch < 0) throw new IllegalStateException();
				add(ch);
			}
		}

		public void eof() {
			add(-1);
		}

		private void add(int ch) {
			switch (ch) {
			case '\n':
				sourceFile.addLineOffset(offset + 1);
				break;
			case '\r':
				// Need to see next character to know if it's a
				// line terminator.
				break;
			default:
				if (lastSeen == '\r') {
					// We consider a bare CR to be an end of line
					// if it is not followed by a new line.
					// Mac OS has historically used a bare CR as
					// its line terminator.
					sourceFile.addLineOffset(offset);
				}
			}

			lastSeen = ch;
			++offset;
		}
	}

	private static final int DEFAULT_SIZE = 100;

	private SourceFileDataSource dataSource;
	private byte[] data;
	private int[] lineNumberMap;
	private int numLines;

	/**
	 * Constructor.
	 *
	 * @param dataSource the SourceFileDataSource object which will
	 *                   provide the data of the source file
	 */
	public SourceFile(SourceFileDataSource dataSource) {
		this.dataSource = dataSource;
		this.lineNumberMap = new int[DEFAULT_SIZE];
		this.numLines = 0;
	}

	/**
	 * Get the full path name of the source file (with directory).
	 */
	public String getFullFileName() {
		return dataSource.getFullFileName();
	}

	/**
	 * Get an InputStream on data.
	 *
	 * @return an InputStream on the data in the source file,
	 *         starting from given offset
	 */
	public InputStream getInputStream() throws IOException {
		loadFileData();
		return new ByteArrayInputStream(data);
	}

	/**
	 * Get an InputStream on data starting at given offset.
	 *
	 * @param offset the start offset
	 * @return an InputStream on the data in the source file,
	 *         starting at the given offset
	 */
	public InputStream getInputStreamFromOffset(int offset) throws IOException {
		loadFileData();
		return new ByteArrayInputStream(data, offset, data.length - offset);
	}

	/**
	 * Add a source line byte offset.
	 * This method should be called for each line in the source file,
	 * in order.
	 *
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
	 *
	 * @param line the line number
	 * @return the byte offset in the file's data for the line,
	 *         or -1 if the line is not valid
	 */
	public int getLineOffset(int line) {
		if (line < 0 || line >= numLines)
			return -1;
		return lineNumberMap[line];
	}

	private synchronized void loadFileData() throws IOException {
		if (data != null)
			return;

		InputStream in = null;

		try {
			in = dataSource.open();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			addLineOffset(0); // Line 0 starts at offset 0
			LineNumberMapBuilder mapBuilder = new LineNumberMapBuilder(this);

			// Copy all of the data from the file into the byte array output stream
			byte[] buf = new byte[1024];
			int n;
			while ((n = in.read(buf)) >= 0) {
				mapBuilder.addData(buf, n);
				out.write(buf, 0, n);
			}
			mapBuilder.eof();

			setData(out.toByteArray());
		} finally {
			if (in != null)
				in.close();
		}

	}

	/**
	 * Set the source file data.
	 *
	 * @param data the data
	 */
	private void setData(byte[] data) {
		this.data = data;
	}
}

// vim:ts=4
