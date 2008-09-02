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

import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;

/**
 * Codebase locator for files and directories in the filesystem.
 * 
 * @author David Hovemeyer
 */
public class FilesystemCodeBaseLocator implements ICodeBaseLocator {
	private final String pathName;

	public FilesystemCodeBaseLocator(String pathName) {
		 File file = new File(pathName);
		 if (!file.exists())
			 throw new IllegalArgumentException("File doesn't exist " + pathName);
		 if (!file.canRead())
			 throw new IllegalArgumentException("Can't read " + pathName);
		try {
	       pathName = file.getCanonicalPath();
        } catch (IOException e) {
	      assert true;
        }
        this.pathName = pathName;
	}

	/**
	 * @return Returns the pathName.
	 */
	public String getPathName() {
		return pathName;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseLocator#createRelativeCodeBaseLocator(java.lang.String)
	 */
	public ICodeBaseLocator createRelativeCodeBaseLocator(String relativePath) {
		File path = new File(pathName);
		if (!path.isDirectory()) {
			path = path.getParentFile();
		}
		File relativeFile = new File(path, relativePath);
		return new FilesystemCodeBaseLocator(relativeFile.getPath());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseLocator#openCodeBase()
	 */
	public ICodeBase openCodeBase() throws IOException {
		return ClassFactory.createFilesystemCodeBase(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "filesystem:" + pathName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		FilesystemCodeBaseLocator other = (FilesystemCodeBaseLocator) obj;
		return this.pathName.equals(other.pathName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return pathName.hashCode();
	}
}
