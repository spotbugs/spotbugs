/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;

/**
 * @author pwilliam
 */
public interface ClassParserInterface {

	/**
	 * Parse the class data into a ClassNameAndSuperclassInfo object containing
	 * (some of) the class's symbolic information.
	 * 
	 * @param classInfo a ClassNameAndSuperclassInfo object to be filled in with (some of)
	 *                   the class's symbolic information
	 * @throws InvalidClassFileFormatException
	 */
	public void parse(ClassNameAndSuperclassInfo.Builder classInfo) throws InvalidClassFileFormatException;

	/**
	 * Parse the class data into a ClassInfo object containing
	 * (some of) the class's symbolic information.
	 * 
	 * @param classInfo a ClassInfo object to be filled in with (some of)
	 *                   the class's symbolic information
	 * @throws InvalidClassFileFormatException
	 */
	public void parse(ClassInfo.Builder classInfo) throws InvalidClassFileFormatException;

}