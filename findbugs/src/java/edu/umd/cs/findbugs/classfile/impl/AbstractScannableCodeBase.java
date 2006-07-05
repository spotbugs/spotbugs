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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Abstract base class for implementations of IScannableCodeBase.
 * Provides an implementation of the
 * getCodeBaseLocator(), containsSourceFiles(),
 * setApplicationCodeBase(), and isApplicationCodeBase() methods.
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractScannableCodeBase implements IScannableCodeBase {
	private ICodeBaseLocator codeBaseLocator;
	private boolean checkedForSourceFiles;
	private boolean containsSourceFiles;
	private boolean isAppCodeBase;
	
	public AbstractScannableCodeBase(ICodeBaseLocator codeBaseLocator) {
		this.codeBaseLocator = codeBaseLocator;
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
		if (!checkedForSourceFiles) {
			ICodeBaseIterator i = iterator();
			while (i.hasNext()) {
				ICodeBaseEntry entry = i.next();
				if (entry.getResourceName().endsWith(".java")) {
					containsSourceFiles = true;
					break;
				}
			}
			checkedForSourceFiles = true;
		}
		return containsSourceFiles;
	}
	
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
}
