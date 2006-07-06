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
 * An engine for analyzing classes or methods.
 * 
 * @author David Hovemeyer
 */
public interface IAnalysisEngine<DescriptorType> {
	/**
	 * Perform an analysis on class or method named by given descriptor.
	 * 
	 * @param analysisCache the analysis cache
	 * @param descriptor    the descriptor of the class or method to be analyzed
	 * @return the result of the analysis of the class or method
	 */
	public Object analyze(IAnalysisCache analysisCache, DescriptorType descriptor)
		throws CheckedAnalysisException;
}
