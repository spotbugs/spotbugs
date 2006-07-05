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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of IClassPath.
 * 
 * @author David Hovemeyer
 */
public class ClassPathImpl implements IClassPath {
	private List<IScannableCodeBase> appCodeBaseList;
	private List<ICodeBase> auxCodeBaseList;
	
	public ClassPathImpl() {
		this.appCodeBaseList = new LinkedList<IScannableCodeBase>();
		this.auxCodeBaseList = new LinkedList<ICodeBase>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#addCodeBase(edu.umd.cs.findbugs.classfile.ICodeBase)
	 */
	public void addCodeBase(ICodeBase codeBase) {
		if (codeBase.isApplicationCodeBase()) {
			if (!(codeBase instanceof IScannableCodeBase)) {
				throw new IllegalStateException();
			}
			appCodeBaseList.add((IScannableCodeBase) codeBase);
		} else {
			auxCodeBaseList.add(codeBase);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#appCodeBaseIterator()
	 */
	public Iterator<? extends ICodeBase> appCodeBaseIterator() {
		return appCodeBaseList.iterator();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#auxCodeBaseIterator()
	 */
	public Iterator<? extends ICodeBase> auxCodeBaseIterator() {
		return auxCodeBaseList.iterator();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#close()
	 */
	public void close() {
		for (ICodeBase codeBase : appCodeBaseList) {
			codeBase.close();
		}
		for (ICodeBase codeBase : auxCodeBaseList) {
			codeBase.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPath#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		for (ICodeBase codeBase : appCodeBaseList) {
			try {
				return codeBase.lookupResource(resourceName);
			} catch (ResourceNotFoundException e) {
				// ignore, continue trying other codebases
			}
		}
		for (ICodeBase codeBase : auxCodeBaseList) {
			try {
				return codeBase.lookupResource(resourceName);
			} catch (ResourceNotFoundException e) {
				// ignore, continue trying other codebases
			}
		}
		throw new ResourceNotFoundException(resourceName);
	}
}
