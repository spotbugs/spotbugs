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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of ICodeBase for a single classfile.
 * 
 * @author David Hovemeyer
 */
public class SingleFileCodeBase implements IScannableCodeBase {
	private boolean isAppCodeBase;
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#setApplicationCodeBase(boolean)
	 */
	public void setApplicationCodeBase(boolean isAppCodeBase) {
		this.isAppCodeBase = isAppCodeBase;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#isApplicationCodeBase()
	 */
	public boolean isApplicationCodeBase() {
		return isAppCodeBase;
	}
	
	/**
	 * @author Dave
	 */
	private final class SingleFileCodeBaseEntry implements ICodeBaseEntry {
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getNumBytes()
		 */
		public int getNumBytes() {
			File file = new File(fileName);
			if (!file.exists()) {
				return -1;
			}
			return (int) file.length();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getResourceName()
		 */
		public String getResourceName() {
			return fileName;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#openResource()
		 */
		public InputStream openResource() throws IOException {
			return openFile();
		}
	}

	private String fileName;
	
	public SingleFileCodeBase(String fileName) {
		this.fileName = fileName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#containsSourceFiles()
	 */
	public boolean containsSourceFiles() throws InterruptedException {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#iterator()
	 */
	public ICodeBaseIterator iterator() throws InterruptedException {
		return new ICodeBaseIterator() {
			boolean done = false;
			
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#hasNext()
			 */
			public boolean hasNext() throws InterruptedException {
				return !done;
			}
			
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
			 */
			public ICodeBaseEntry next() throws InterruptedException {
				if (done) {
					throw new NoSuchElementException();
				}
				return new SingleFileCodeBaseEntry();
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		if (!resourceName.equals(fileName)) {
			throw new ResourceNotFoundException(resourceName);
		}
		return new SingleFileCodeBaseEntry();
	}
	
	private InputStream openFile() throws IOException {
		return new BufferedInputStream(new FileInputStream(fileName));
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		// Nothing to do
	}
}
