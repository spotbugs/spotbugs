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

package edu.umd.cs.findbugs.classfile;

/**
 * Exception to indicate that the class name defined in
 * a class file does not match its expected class name
 * (as indicated by its resource name).
 * 
 * @author David Hovemeyer
 */
public class ClassNameMismatchException extends InvalidClassFileFormatException {
	private ClassDescriptor loadedClassDescriptor;
	
	/**
	 * Constructor.
	 * 
	 * @param expectedClassDescriptor class descriptor we were expected based on the resource name
	 * @param loadedClassDescriptor   class descriptor actually found in the class file
	 * @param codeBaseEntry           codebase entry the class was loaded from
	 */
	public ClassNameMismatchException(
			ClassDescriptor expectedClassDescriptor,
			ClassDescriptor loadedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		super("Expected class name " +
				expectedClassDescriptor +
				" does not match loaded class name " +
				loadedClassDescriptor,
				expectedClassDescriptor,
				codeBaseEntry);
		this.loadedClassDescriptor = loadedClassDescriptor;
	}
	
	/**
	 * @return Returns the loadedClassDescriptor.
	 */
	public ClassDescriptor getLoadedClassDescriptor() {
		return loadedClassDescriptor;
	}

}
