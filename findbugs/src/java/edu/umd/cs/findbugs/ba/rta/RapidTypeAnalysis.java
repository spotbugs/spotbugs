/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2006 University of Maryland
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

package edu.umd.cs.findbugs.ba.rta;

import java.util.HashSet;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassObserver;

/**
 * Driver for performing Rapid Type Analysis (RTA) on a collection of
 * classes.  RTA is an algorithm devised by David Bacon to compute
 * an accurate call graph for an object-oriented program.
 */
@Deprecated
public class RapidTypeAnalysis implements IClassObserver {
	// Set of classes observed.
	private HashSet<JavaClass> observedClassSet;

	/**
	 * Constructor.
	 */
	@Deprecated
	public RapidTypeAnalysis() {
		this.observedClassSet = new HashSet<JavaClass>();
	}

	public void execute() {
		// TODO: implement
	}

	public void observeClass(ClassDescriptor classDescriptor) {
		try {
			JavaClass javaClass = AnalysisContext.currentAnalysisContext().lookupClass(classDescriptor);
			observedClassSet.add(javaClass);
			throw new UnsupportedOperationException();
		} catch (ClassNotFoundException e) {
			// Shouldn't happen
		}
	}

}

// vim:ts=4
