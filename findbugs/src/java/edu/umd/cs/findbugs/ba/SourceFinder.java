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

import java.util.*;
import java.io.*;

/**
 * Class to open input streams on source files.
 * It maintains a "source path", which is like a classpath,
 * but for finding source files instead of class files.
 */
public class SourceFinder {
	private static final boolean DEBUG = Boolean.getBoolean("srcfinder.debug");

	private static final int CACHE_SIZE = 50;

	private static class Cache extends LinkedHashMap<String, SourceFile> {
		protected boolean removeEldestEntry(Map.Entry<String, SourceFile> eldest) {
			return size() >= CACHE_SIZE;
		}
	}

	private List<String> sourceBaseList;
	private Cache cache;

	private static int intValueOf(byte b) {
		// Why isn't there an API method to do this?
		if ((b & 0x80) == 0)
			return b;
		else
			return 0x80 | ((int)b & 0x7F);
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

	/**
	 * Constructor.
	 * @param path the source path, in the same format as a classpath
	 */
	public SourceFinder() {
		sourceBaseList = null;
		cache = new Cache();
	}

	/**
	 * Set the list of source directories.
	 */
	public void setSourceBaseList(List<String> sourceBaseList) {
		this.sourceBaseList = sourceBaseList;
	}

	/**
	 * Open an input stream on a source file in given package.
	 * @param packageName the name of the package containing the class whose source file is given
	 * @param fileName the unqualified name of the source file
	 * @return an InputStream on the source file
	 * @throws IOException if a matching source file cannot be found
	 */
	public InputStream openSource(String packageName, String fileName) throws IOException {
		SourceFile sourceFile = findSourceFile(packageName, fileName);
		return sourceFile.getInputStream();
	}

	/**
	 * Open a source file in given package.
	 * @param packageName the name of the package containing the class whose source file is given
	 * @param fileName the unqualified name of the source file
	 * @return the source file
	 * @throws IOException if a matching source file cannot be found
	 */
	public SourceFile findSourceFile(String packageName, String fileName) throws IOException {
		// Create a fully qualified source filename using the package name.
		StringBuffer fullName = new StringBuffer();
		if (!packageName.equals("")) {
			fullName.append(packageName.replace('.', File.separatorChar));
			fullName.append(File.separatorChar);
		}
		fullName.append(fileName);
		fileName = fullName.toString();

		// Is the file in the cache already?
		SourceFile sourceFile = cache.get(fileName);
		if (sourceFile == null) {
			 // Find this source file, add its data to the cache
			 if (DEBUG) System.out.println("Trying "  + fileName + "...");

			// Query each element of the source path to find the requested source file
			Iterator<String> i = sourceBaseList.iterator();		
			while (i.hasNext()) {
				String sourceBase = i.next();

				// Try to read the file from current source base element
				String fullFileName = sourceBase + File.separator + fileName;
				if (DEBUG) System.out.println("Trying " + fullFileName + "...");

				InputStream in = null;

				try {
					in = new BufferedInputStream(new FileInputStream(fullFileName));
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					sourceFile = new SourceFile();
					sourceFile.addLineOffset(0); // Line 0 starts at offset 0
					LineNumberMapBuilder mapBuilder = new LineNumberMapBuilder(sourceFile);

					// Copy all of the data from the file into the byte array output stream
					byte[] buf = new byte[1024];
					int n;
					while ((n = in.read(buf)) >= 0) {
						mapBuilder.addData(buf, n);
						out.write(buf, 0, n);
					}
					mapBuilder.eof();

					sourceFile.setData(out.toByteArray());
				} catch (FileNotFoundException e) {
					// We're probably looking in the wrong directory -
					// just ignore the failure and continue with the loop
				} finally {
					if (in != null)
						in.close();
				}

				if (sourceFile != null) {
					// Success!
					// Put the data for the file in the cache
					cache.put(fileName, sourceFile);
					break;
				}
			}

			// Couldn't find the source file.
			if (sourceFile == null)
				throw new FileNotFoundException("Can't find source file " + fileName);
		}

		return sourceFile;
	}

}

// vim:ts=4
