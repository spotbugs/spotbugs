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

import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

final class DirectoryCodeBaseEntry implements ICodeBaseEntry {
	private final DirectoryCodeBase codeBase;
	private final String resourceName;

	DirectoryCodeBaseEntry(DirectoryCodeBase codeBase, String resourceName) {
		this.codeBase = codeBase;
		this.resourceName = resourceName;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getNumBytes()
	 */
	public int getNumBytes() {
		File fullPath = codeBase.getFullPathOfResource(resourceName);
		if (!fullPath.exists()) {
			return -1;
		}
		return (int) fullPath.length();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getResourceName()
	 */
	public String getResourceName() {
		return resourceName;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#openResource()
	 */
	public InputStream openResource() throws IOException {
		return codeBase.openFile(resourceName);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getCodeBase()
	 */
	public ICodeBase getCodeBase() {
		return codeBase;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		DirectoryCodeBaseEntry other = (DirectoryCodeBaseEntry) obj;
		return this.codeBase.equals(other.codeBase)
			&& this.resourceName.equals(other.resourceName);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 7919 * codeBase.hashCode() + resourceName.hashCode();
	}
}
