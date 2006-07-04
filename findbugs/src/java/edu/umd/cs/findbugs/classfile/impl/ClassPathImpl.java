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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of IClassPath.
 * 
 * @author David Hovemeyer
 */
public class ClassPathImpl implements IClassPath {
	private List<ICodeBase> codeBaseList;

	/**
	 * Constructor.
	 * Creates an empty classpath.
	 */
	public ClassPathImpl() {
		this.codeBaseList = new LinkedList<ICodeBase>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#addCodeBase(edu.umd.cs.findbugs.classfile.ICodeBase)
	 */
	public void addCodeBase(ICodeBase codeBase) {
		codeBaseList.add(codeBase);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		for (ICodeBase codeBase : codeBaseList) {
			try {
				return codeBase.lookupResource(resourceName);
			} catch (ResourceNotFoundException e) {
				// do nothing, continue with next code base, if any
			}
		}
		throw new ResourceNotFoundException(resourceName);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#close()
	 */
	public void close() {
		for (ICodeBase codeBase : codeBaseList) {
			codeBase.close();
		}
	}
}
