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

	private static final int CACHE_SIZE = 50;

	private static class Cache extends LinkedHashMap<String, byte[]> {
		protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
			return size() >= CACHE_SIZE;
		}
	}

	private List<String> sourceBaseList;
	private Cache cache;

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
	 * Open an input stream on a source file for a class in given package.
	 * @param packageName the name of the package containing the class whose source file is given
	 * @param fileName the unqualified name of the source file
	 * @throws IOException if a matching source file cannot be found
	 */
	public InputStream openSource(String packageName, String fileName) throws IOException {
		// Create a fully qualified source filename using the package name.
		StringBuffer fullName = new StringBuffer();
		if (!packageName.equals("")) {
			fullName.append(packageName.replace('.', File.separatorChar));
			fullName.append(File.separatorChar);
		}
		fullName.append(fileName);
		fileName = fullName.toString();

		// Is the file in the cache already?
		byte[] data = cache.get(fileName);
		if (data == null) {
			 // Find this source file, add its data to the cache
			 //System.out.println("Trying "  + fileName + "...");

			// Query each element of the source path to find the requested source file
			Iterator<String> i = sourceBaseList.iterator();		
			while (i.hasNext()) {
				String sourceBase = i.next();

				// Try to read the file from current source base element
				String fullFileName = sourceBase + File.separator + fileName;
				//System.out.println("Trying " + fullFileName + "...");

				InputStream in = null;

				try {
					in = new BufferedInputStream(new FileInputStream(fullFileName));
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					// Copy all of the data from the file into the byte array output stream
					byte[] buf = new byte[1024];
					int n;
					while ((n = in.read(buf)) >= 0) {
						out.write(buf, 0, n);
					}

					data = out.toByteArray();

				} finally {
					if (in != null)
						in.close();
				}

				if (data != null) {
					// Success!
					// Put the data for the file in the cache
					cache.put(fileName, data);
					break;
				}
			}

			// Couldn't find the source file.
			if (data == null)
				throw new FileNotFoundException("Can't find source file " + fileName);
		}

		return new ByteArrayInputStream(data);
	}

}

// vim:ts=4
