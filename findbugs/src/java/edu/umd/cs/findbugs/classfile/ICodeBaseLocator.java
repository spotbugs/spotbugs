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

/**
 * Specify the location of a codebase.
 * 
 * @author David Hovemeyer
 */
public interface ICodeBaseLocator {
	/**
	 * Get the codebase object.
	 * 
	 * @return the codebase object
	 */
	public ICodeBase openCodeBase() throws IOException, ResourceNotFoundException;
	
	/**
	 * Get the codebase locator describing the location of
	 * a relative codebase.  This method is useful for getting the
	 * location of a codebase referred to in the Class-Path attribute
	 * of a Jar manifest.
	 * 
	 * @param relativePath the path of a relative codebase
	 * @return codebase locator of the relative codebase whose path is given
	 */
	public ICodeBaseLocator createRelativeCodeBaseLocator(String relativePath);
	
	/**
	 * Convert the codebase locator to a string representation.
	 * If possible two codebase locators that refer to the same codebase
	 * should produce the same string representation.
	 * So, this string can serve as a key identifying the codebase
	 * in a map.
	 * 
	 * @return a string representation of the codebase
	 */
	public String toString();
}
