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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	private int howDiscovered;
	private long lastModifiedTime;
	private Map<String, String> resourceNameTranslationMap;
	
	public AbstractScannableCodeBase(ICodeBaseLocator codeBaseLocator) {
		this.codeBaseLocator = codeBaseLocator;
		this.lastModifiedTime = -1L;
		this.resourceNameTranslationMap = new HashMap<String, String>();
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
		// MUSTFIX
		if (true) return false;
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
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#setHowDiscovered(int)
	 */
	public void setHowDiscovered(int howDiscovered) {
		this.howDiscovered = howDiscovered;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getHowDiscovered()
	 */
	public int getHowDiscovered() {
		return howDiscovered;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#setLastModifiedTime(long)
	 */
	public void setLastModifiedTime(long lastModifiedTime) {
		if (lastModifiedTime > 0) {
			this.lastModifiedTime = lastModifiedTime;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getLastModifiedTime()
	 */
	public long getLastModifiedTime() {
		return lastModifiedTime;
	}
	
	public void addResourceNameTranslation(String origResourceName, String newResourceName) {
		resourceNameTranslationMap.put(origResourceName, newResourceName);
	}
	
	public String translateResourceName(String resourceName) {
		String translatedName = resourceNameTranslationMap.get(resourceName);
		return translatedName != null ? translatedName : resourceName;
	}
}
