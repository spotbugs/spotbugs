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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * IScannableCodeBase implementation to read resources
 * from a filesystem directory.
 * 
 * @author David Hovemeyer
 */
public class DirectoryCodeBase extends AbstractScannableCodeBase implements IScannableCodeBase {
	private File directory;
	
	public DirectoryCodeBase(File directory) {
		if (!directory.isDirectory()) throw new IllegalArgumentException();
		this.directory = directory;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#iterator()
	 */
	public ICodeBaseIterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#openResource(java.lang.String)
	 */
	public InputStream openResource(String resourceName)
			throws ResourceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
