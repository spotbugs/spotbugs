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
	// capabilities:
	// open stream for named resource (resources are named by strings)
	
	/**
	 * Open an InputStream on named resource.
	 * 
	 * @param resourceName the name of the resource to open
	 * @return an InputStream reading the named resource
	 * @throws ResourceNotFoundException if the named resource is not found
	 * @throws IOException if the named resource exists but cannot be opened
	 */
	public InputStream openResource(String resourceName) throws ResourceNotFoundException, IOException;
	
	/**
	 * This method should be called when done using the code base.
	 */
	public void close();
}
