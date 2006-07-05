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
 * Descriptor for a Java class.
 * Each reachable class in the application and its aux classpath is
 * represented uniquely by an instance of IClass.
 * 
 * @author David Hovemeyer
 */
public interface IClassDescriptor {
	/**
	 * Get the name of the class.
	 * 
	 * @return the name of the class
	 */
	public String getClassName();
	
	/**
	 * Get the codebase entry from which the class data was loaded.
	 * 
	 * @return the codebase entry from which the class data was loaded
	 */
	public ICodeBaseEntry getCodeBaseEntry();
}
