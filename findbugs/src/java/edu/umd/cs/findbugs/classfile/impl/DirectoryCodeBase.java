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

import edu.umd.cs.findbugs.RecursiveFileSearch;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * IScannableCodeBase implementation to read resources
 * from a filesystem directory.
 * 
 * @author David Hovemeyer
 */
public class DirectoryCodeBase extends AbstractScannableCodeBase implements IScannableCodeBase {
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
			
			return new DirectoryCodeBaseEntry(DirectoryCodeBase.this, fileName);
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
		if (!directory.isDirectory()) throw new IllegalArgumentException();
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
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		// Nothing to do
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		File file = getFullPathOfResource(resourceName);
		if (!file.exists()) {
			throw new ResourceNotFoundException(resourceName);
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
}
