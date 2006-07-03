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

import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Abstract base class for implementations of IScannableCodeBase.
 * Provides an implementation of the containsSourceFiles() method.
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractScannableCodeBase implements IScannableCodeBase {
	private boolean checkedForSourceFiles;
	private boolean containsSourceFiles;

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#containsSourceFiles()
	 */
	public boolean containsSourceFiles() {
		if (!checkedForSourceFiles) {
			Iterator<String> i = resourceNameIterator();
			while (i.hasNext()) {
				String resourceName = i.next();
				if (resourceName.endsWith(".java")) {
					containsSourceFiles = true;
					break;
				}
			}
			checkedForSourceFiles = true;
		}
		return containsSourceFiles;
	}
}
