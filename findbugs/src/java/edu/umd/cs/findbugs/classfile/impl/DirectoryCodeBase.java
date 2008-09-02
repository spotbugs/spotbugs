/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

import edu.umd.cs.findbugs.RecursiveFileSearch;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;

/**
 * IScannableCodeBase implementation to read resources
 * from a filesystem directory.
 *
 * @author David Hovemeyer
 */
public class DirectoryCodeBase extends AbstractScannableCodeBase  {
	private class DirectoryCodeBaseIterator implements ICodeBaseIterator {

		Iterator<String> fileNameIterator = rfs.fileNameIterator();

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#hasNext()
		 */
		public boolean hasNext() throws InterruptedException {
			return fileNameIterator.hasNext();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
		 */
		public ICodeBaseEntry next() throws InterruptedException {
			final String fileName = fileNameIterator.next();

			// Make the filename relative to the directory
			String resourceName = getResourceName(fileName);

			// Update last modified time
			File file = new File(fileName);
			long modTime = file.lastModified();
			if (modTime > 0 && modTime > getLastModifiedTime()) {
				setLastModifiedTime(modTime);
			}

			return new DirectoryCodeBaseEntry(DirectoryCodeBase.this, resourceName);
		}
	}

	private File directory;
	private RecursiveFileSearch rfs;
	private boolean searchPerformed;

	/**
	 * Constructor.
	 *
	 * @param codeBaseLocator the codebase locator for this codebase
	 * @param directory       the filesystem directory
	 */
	public DirectoryCodeBase(ICodeBaseLocator codeBaseLocator, File directory) {
		super(codeBaseLocator);
		if (!directory.isDirectory()) {
	        throw new IllegalArgumentException();
        }
		this.directory = directory;
		this.rfs = new RecursiveFileSearch(directory.getPath(), new FileFilter(){
			/* (non-Javadoc)
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File pathname) {
				return true;
			}
		});
		this.searchPerformed = false;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#iterator()
	 */
	public ICodeBaseIterator iterator() throws InterruptedException {
		if (!searchPerformed) {
			rfs.search();
			searchPerformed = true;
		}

		return new DirectoryCodeBaseIterator();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getPathName()
	 */
	public String getPathName() {
		return directory.getPath();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		// Nothing to do
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) {
		// Translate resource name, in case a resource name
		// has been overridden and the resource is being accessed
		// using the overridden name.
		resourceName = translateResourceName(resourceName);

		File file = getFullPathOfResource(resourceName);
		if (!file.exists()) {
			return null;
		}
		return new DirectoryCodeBaseEntry(this, resourceName);
	}

	InputStream openFile(String resourceName) throws FileNotFoundException, IOException {
		File path = getFullPathOfResource(resourceName);
		return new BufferedInputStream(new FileInputStream(path));
	}

	/**
	 * Get the full path of given resource.
	 *
	 * @param resourceName
	 * @return
	 */
	File getFullPathOfResource(String resourceName) {
		return new File(directory, resourceName);
	}

	/**
	 * Get the resource name given a full filename.
	 *
	 * @param fileName the full filename (which must be inside the directory)
	 * @return the resource name (i.e., the filename with the directory stripped off)
	 */
	String getResourceName(String fileName) {
		// FIXME: there is probably a more robust way to do this

		// Strip off the directory part.
		String dirPath = directory.getPath();
		if (!fileName.startsWith(dirPath)) {
			throw new IllegalStateException("Filename " + fileName + " not inside directory "+ dirPath);
		}

		// The problem here is that we need to take the relative part of the filename
		// and break it into components that we can then reconstruct into
		// a resource name (using '/' characters to separate the components).
		// Unfortunately, the File class does not make this task particularly easy.

		String relativeFileName = fileName.substring(dirPath.length());
		File file = new File(relativeFileName);
		LinkedList<String> partList = new LinkedList<String>();
		do {
			partList.addFirst(file.getName());
		} while ((file = file.getParentFile()) != null);

		StringBuilder buf = new StringBuilder();
		for (String part : partList) {
			if (buf.length() > 0) {
				buf.append('/');
			}
			buf.append(part);
		}

		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return directory.getPath();
	}
}
