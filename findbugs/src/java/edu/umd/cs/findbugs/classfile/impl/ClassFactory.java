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
import java.net.MalformedURLException;
import java.net.URL;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;

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
		// FIXME: support single files
		// FIXME: check for URL protocol, reject non-file URLs
		// TODO: support remote archives?
		
		File file = new File(fileName);
		
		if (file.isDirectory()) {
			return new DirectoryCodeBase(file);
		} else {
			return new ZipFileCodeBase(file);
		}
	}
	
	public ICodeBase createRemoteCodeBase(String urlSpec) throws MalformedURLException {
		// TODO
		return null;
	}
}
