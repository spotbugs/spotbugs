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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Detector interface for new bytecode-framework-neutral architecture.
 * 
 * @author David Hovemeyer
 */
public interface Detector2 extends Priorities {

	/**
	 * Visit a class.
	 * 
	 * @param classDescriptor descriptor naming the class to visit
	 * @throws CheckedAnalysisException if an exception occurs during analysis
	 */
	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException;

	/**
	 * This method is called at the end of the analysis pass.
	 */
	public void finishPass();

	/**
	 * Get the name of the detector class.
	 * 
	 * @return the name of the detector class.
	 */
	public String getDetectorClassName();
}
