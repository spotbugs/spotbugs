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
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.engine.ClassInfoAnalysisEngine;
import edu.umd.cs.findbugs.io.IO;

/**
 * Implementation of ICodeBase for a single classfile.
 * 
 * @author David Hovemeyer
 */
public class SingleFileCodeBase implements IScannableCodeBase {
	
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
	
	private ICodeBaseLocator codeBaseLocator;
	private String fileName;
	private boolean isAppCodeBase;
	private boolean resourceNameKnown;
	private String resourceName;
	
	public SingleFileCodeBase(ICodeBaseLocator codeBaseLocator, String fileName) {
		this.codeBaseLocator = codeBaseLocator;
		this.fileName = fileName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getCodeBaseLocator()
	 */
	public ICodeBaseLocator getCodeBaseLocator() {
		return codeBaseLocator;
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
				done = true;
				return new SingleFileCodeBaseEntry(SingleFileCodeBase.this);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		if (!resourceName.equals(getResourceName())) {
			throw new ResourceNotFoundException(resourceName);
		}
		
		return new SingleFileCodeBaseEntry(this);
	}
	
	InputStream openFile() throws IOException {
		return new BufferedInputStream(new FileInputStream(fileName));
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		// Nothing to do
	}

	/**
	 * Get the resource name of the single file.
	 * We have to open the file and parse the constant pool
	 * in order to find this out.
	 * 
	 * @return the resource name (e.g., "java/lang/String.class"
	 *          if the class is java.lang.String)
	 */
	String getResourceName() {
		if (!resourceNameKnown) {
			// The resource name of a classfile can only be determined by reading
			// the file and parsing the constant pool.
			// If we can't do this for some reason, then we just
			// make the resource name equal to the filename.
			
			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(fileName));
				ClassInfo classInfo = ClassInfoAnalysisEngine.parseClassInfo(
						null, new SingleFileCodeBaseEntry(this), in);
				resourceName = classInfo.getClassDescriptor().toResourceName();
			} catch (IOException e) {
				resourceName = fileName;
			} catch (CheckedAnalysisException e) {
				resourceName = fileName;
			} finally {
				if (in != null) {
					IO.close(in);
				}
			}
			resourceNameKnown = true;
		}
		return resourceName;
	}

	/**
	 * Return the number of bytes in the file.
	 * 
	 * @return the number of bytes in the file, or -1 if the file's length
	 *          can't be determined
	 */
	int getNumBytes() {
		File file = new File(fileName);
		if (!file.exists()) {
			return -1;
		}
		return (int) file.length();
	}
}
