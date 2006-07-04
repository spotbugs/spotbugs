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

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Factory to create codebase/classpath/classfile objects. 
 * 
 * @author David Hovemeyer
 */
public class ClassFactory {
	private static ClassFactory theInstance = new ClassFactory();
	
	private ClassFactory() {
	}
	
	public static ClassFactory instance() {
		return theInstance;
	}
	
	public IClassPath createClassPath() {
		return new ClassPathImpl();
	}
	
	public IScannableCodeBase createLocalCodeBase(String fileName) throws IOException {
		// FIXME: check for URL protocol, reject non-file URLs
		// TODO: support remote archives?
		
		File file = new File(fileName);
		
		if (file.isDirectory()) {
			return new DirectoryCodeBase(file);
		} else if (fileName.endsWith(".class")) {
			return new SingleFileCodeBase(fileName);
		} else {
			return new ZipFileCodeBase(file);
		}
	}
	
	public IScannableCodeBase createNestedArchiveCodeBase(
			IScannableCodeBase parentCodeBase, String resourceName)
			throws ResourceNotFoundException, IOException {
		return new NestedZipFileCodeBase(parentCodeBase, resourceName);
	}
}
