/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

import edu.umd.cs.findbugs.ba.ClassObserver;
import org.apache.bcel.classfile.JavaClass;

/**
 * Driver for performing Rapid Type Analysis (RTA) on a collection of
 * classes.  RTA is an algorithm devised by David Bacon to compute
 * an accurate call graph for an object-oriented program.
 */
public class RapidTypeAnalysis implements ClassObserver {
	// Set of classes observed.
	private HashSet<JavaClass> observedClassSet;

	/**
	 * Constructor.
	 */
	public RapidTypeAnalysis() {
		this.observedClassSet = new HashSet<JavaClass>();
	}

	public void execute() {
		// TODO: implement
	}

	public void observeClass(JavaClass javaClass) {
		observedClassSet.add(javaClass);
	}

}

// vim:ts=4
