/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import java.util.*;
import java.io.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * Detector for enumerating simple paths through all of the methods of a class.
 * An abstract method is called to create an InstructionScannerGenerator
 * to analyze the paths.
 */
public abstract class SimplePathEnumeratingDetector extends CFGBuildingDetector
	implements Detector {

	private static final boolean DEBUG = Boolean.getBoolean("spev.debug");

	/**
	 * Default number of simple paths considered per-method if not specified
	 * explicitly and the <code>findbugs.maxpaths</code> property is not set.
	 */
	public static final int DEFAULT_MAX_PATHS = 256;

	private int maxPaths;

	/**
	 * Constructor.
	 * @param maxPaths the maximum number of simple paths that will be considered per-method
	 */
	public SimplePathEnumeratingDetector(int maxPaths) {
		this.maxPaths = maxPaths;
	}

	/**
	 * Constructor.
	 * The maximum number of paths that will be considered per-method is
	 * determined from the <code>findbugs.maxpaths</code> property, or,
	 * if that property is not set, the <code>DEFAULT_MAX_PATHS</code> constant.
	 */
	public SimplePathEnumeratingDetector() {
		this.maxPaths = Integer.getInteger("findbugs.maxpaths", DEFAULT_MAX_PATHS).intValue();
	}

	/**
	 * Enumerate simple paths on this method (whose CFG and MethodGen are given),
	 * invoking an InstructionScanner on each simple path.
	 * @param cfg the method's control flow graph
	 * @param mg the method's MethodGen
	 */
	public void visitCFG(CFG cfg, MethodGen mg) {
		// Enumerate simple paths through this method
		Iterator<List<Edge>> j = new SimplePathEnumerator(cfg, maxPaths).enumerate().iterator();
		int count = 0;
		while (j.hasNext()) {
			List<Edge> edgeList = j.next();

			// Create an InstructionScannerGenerator for analyzing this path
			InstructionScannerGenerator generator = createInstructionScannerGenerator(mg);

			// Pump the instructions in the path through the scanner generator
			// and all of the scanners it generates.
			if (DEBUG) System.out.println("=============== Scanning ==================");
			new InstructionScannerDriver(edgeList.iterator()).execute(generator);

			++count;
		}

		if (DEBUG) System.out.println("Examined " + count + " simple paths");
	}

	/**
	 * Create an InstructionScannerGenerator implementing the state machine
	 * that will be checked over the enumerated paths.
	 */
	public abstract InstructionScannerGenerator createInstructionScannerGenerator(MethodGen methodGen);
}

// vim:ts=4
