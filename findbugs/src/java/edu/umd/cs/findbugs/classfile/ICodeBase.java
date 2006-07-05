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

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Interface for a basic code base in which we can look up resources
 * but not necessarily scan for the list of all resources.
 * 
 * @author David Hovemeyer
 */
public interface ICodeBase {
	/**
	 * Get the codebase locator describing the location of this codebase.
	 * 
	 * @return the ICodeBaseLocator
	 */
	public ICodeBaseLocator getCodeBaseLocator();
	
	/**
	 * Look up a resource in this code base.
	 * 
	 * @param resourceName name of the resource to look up
	 * @return ICodeBaseEntry representing the resource
	 * @throws ResourceNotFoundException if the resource cannot be found in this code base
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException;

	/**
	 * Designate this code base as an application codebase.
	 * 
	 * @param isAppCodeBase true if this is an application codebase, false if not
	 */
	public void setApplicationCodeBase(boolean isAppCodeBase);
	
	/**
	 * Return whether or not this codebase is an application codebase.
	 * 
	 * @return true if this is an application codebase, false if not
	 */
	public boolean isApplicationCodeBase();
	
	/**
	 * This method should be called when done using the code base.
	 */
	public void close();
}
