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

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Build a classpath.
 * Takes a list of project codebases and
 * <ul>
 * <li>Scans them for nested and referenced codebases</li>
 * <li>Builds a list of application class descriptors</li>
 * <li>Adds system codebases</li>
 * </ul>
 * 
 * @author David Hovemeyer
 */
public interface IClassPathBuilder {
	/**
	 * Add a project codebase.
	 * 
	 * @param locator       locator for project codebase
	 * @param isApplication true if the codebase is an application codebase, false otherwise
	 */
	public void addCodeBase(ICodeBaseLocator locator, boolean isApplication);

	/**
	 * Build the classpath.
	 * 
	 * @param classPath IClassPath object to build
	 * @param progress  IClassPathBuilderProgress callback
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void build(IClassPath classPath, IClassPathBuilderProgress progress) throws ResourceNotFoundException, IOException, InterruptedException;
	
	/**
	 * Get the list of application classes discovered while scanning the classpath.
	 * 
	 * @return list of application classes
	 */
	public List<ClassDescriptor> getAppClassList();
	
//	/**
//	 * Get the set of all classes discovered while scanning the classpath.
//	 * 
//	 * @return the set of all classes discovered while scanning the classpath
//	 */
//	public Set<ClassDescriptor> getAllClassSet();
}
