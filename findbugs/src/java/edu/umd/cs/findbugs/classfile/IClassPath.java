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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A classpath from which resources (classes and other files)
 * may be loaded.  Essentially, this is just a list of codebases.
 * 
 * @author David Hovemeyer
 */
public interface IClassPath {
	
	/**
	 * Add a CodeBase to the class path.
	 * The order in which code bases are added is the
	 * order in which they will be tried when a resource is
	 * requested.
	 * 
	 * @param codeBase
	 */
	public void addCodeBase(ICodeBase codeBase);

	/**
	 * Open an input stream to read from named resource.
	 * The code bases that are part of the classpath are tried
	 * in order.
	 * 
	 * @param resourceName the name of the resource to open
	 * @return an InputStream reading from the code base
	 * @throws ResourceNotFoundException if the named resource cannot be found
	 * @throws IOException if the named resource is found but cannot be opened
	 */
	public InputStream openResource(String resourceName) throws ResourceNotFoundException, IOException;
	
	/**
	 * Close all of the code bases that are part of this class path.
	 * This should be done once the client is finished with the classpath.
	 */
	public void close();

}
