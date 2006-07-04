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

import java.util.Iterator;

/**
 * A scannable code base: in addition to looking up a named resource,
 * scannable code bases can also enumerate the names of the resources they
 * contain.
 * 
 * @author David Hovemeyer
 */
public interface IScannableCodeBase extends ICodeBase {
	// enumerate resources (classes and files)
	// query whether or not the code base contains source files
	// [get URL?]

//	/**
//	 * Get an Iterator over the filenames of the resources in this code base.
//	 * 
//	 * @return Iterator over the filenames of the resources in this code base
//	 */
//	public Iterator<String> resourceNameIterator();

	/**
	 * Get an iterator over the resources in the this code base.
	 * 
	 * @return ICodeBaseIterator over the resources in the code base
	 */
	public ICodeBaseIterator iterator();
	
	/**
	 * Return whether or not this code base contains any source files.
	 * 
	 * @return true if the code base contains source file(s),
	 *          false if it does not contain source files
	 */
	public boolean containsSourceFiles() throws InterruptedException;
}
