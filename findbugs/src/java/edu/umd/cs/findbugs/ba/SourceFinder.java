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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.*;

/**
 * Class to open input streams on source files.
 * It maintains a "source path", which is like a classpath,
 * but for finding source files instead of class files.
 */
public class SourceFinder {
	private static final boolean DEBUG = Boolean.getBoolean("srcfinder.debug");
	private static final int CACHE_SIZE = 50;

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * Cache of SourceFiles.
	 * We use this to avoid repeatedly having to read
	 * frequently accessed source files.
	 */
	private static class Cache extends LinkedHashMap<String, SourceFile> {
		protected boolean removeEldestEntry(Map.Entry<String, SourceFile> eldest) {
			return size() >= CACHE_SIZE;
		}
	}

	/**
	 * A repository of source files.
	 */
	private interface SourceRepository {
		public boolean contains(String fileName);

		public SourceFileDataSource getDataSource(String fileName);
	}

	/**
	 * A directory containing source files.
	 */
	private static class DirectorySourceRepository implements SourceRepository {
		private String baseDir;

		public DirectorySourceRepository(String baseDir) {
			this.baseDir = baseDir;
		}

		public boolean contains(String fileName) {
			File file = new File(getFullFileName(fileName));
			return file.exists();
		}

		public SourceFileDataSource getDataSource(String fileName) {
			return new FileSourceFileDataSource(getFullFileName(fileName));
		}

		private String getFullFileName(String fileName) {
			return baseDir + File.separator + fileName;
		}
	}

	/**
	 * A zip or jar archive containing source files.
	 */
	private static class ZipSourceRepository implements SourceRepository {
		private ZipFile zipFile;

		public ZipSourceRepository(ZipFile zipFile) {
			this.zipFile = zipFile;
		}

		public boolean contains(String fileName) {
			return zipFile.getEntry(fileName) != null;
		}

		public SourceFileDataSource getDataSource(String fileName) {
			return new ZipSourceFileDataSource(zipFile, fileName);
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private List<SourceRepository> repositoryList;
	private Cache cache;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 */
	public SourceFinder() {
		repositoryList = new LinkedList<SourceRepository>();
		cache = new Cache();
	}

	/**
	 * Set the list of source directories.
	 */
	public void setSourceBaseList(List<String> sourceBaseList) {
		Iterator<String> i = sourceBaseList.iterator();
		while (i.hasNext()) {
			String repos = i.next();
			if (repos.endsWith(".zip") || repos.endsWith(".jar")) {
				// Zip or jar archive
				try {
					ZipFile zipFile = new ZipFile(repos);
					repositoryList.add(new ZipSourceRepository(zipFile));
				} catch (IOException e) {
					// Ignored - we won't use this archive
				}
			} else {
				// Directory
				repositoryList.add(new DirectorySourceRepository(repos));
			}
		}
	}

	/**
	 * Open an input stream on a source file in given package.
	 *
	 * @param packageName the name of the package containing the class whose source file is given
	 * @param fileName    the unqualified name of the source file
	 * @return an InputStream on the source file
	 * @throws IOException if a matching source file cannot be found
	 */
	public InputStream openSource(String packageName, String fileName) throws IOException {
		SourceFile sourceFile = findSourceFile(packageName, fileName);
		return sourceFile.getInputStream();
	}

	/**
	 * Open a source file in given package.
	 *
	 * @param packageName the name of the package containing the class whose source file is given
	 * @param fileName    the unqualified name of the source file
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
			if (DEBUG) System.out.println("Trying " + fileName + "...");

			// Query each element of the source path to find the requested source file
			Iterator<SourceRepository> i = repositoryList.iterator();
			while (i.hasNext()) {
				SourceRepository repos = i.next();

				if (repos.contains(fileName)) {
					// Found it
					sourceFile = new SourceFile(repos.getDataSource(fileName));
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
