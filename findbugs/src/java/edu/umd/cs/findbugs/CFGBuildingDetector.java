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

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * Visit all of the concrete methods in a class, building a control flow graph
 * for each one.
 */
public abstract class CFGBuildingDetector implements Detector {

	private static final boolean DEBUG = Boolean.getBoolean("cbv.debug");
	private static final boolean PRINTCFG = Boolean.getBoolean("cbv.printcfg");
	private static final boolean NOSCREEN = Boolean.getBoolean("cbv.noscreen");

	private ClassContext classContext;

	/**
	 * Visit ClassContext for a class.
	 * The class context caches MethodGens and CFGs for a class,
	 * so each visitor doesn't have to reconstruct them from scratch.
	 * Build a control flow graph for each concrete method in the
	 * class, and invoke the visitCFG() method on it.
	 *
	 * @param classContext the ClassContext
	 */
	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;

		startClass(classContext);

		JavaClass jclass = classContext.getJavaClass();

		Method[] methods = jclass.getMethods();
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];
			if (method.isAbstract() || method.isNative())
				continue;

			// Get the MethodGen for the method from the ClassContext
			MethodGen mg = classContext.getMethodGen(method);
			if (DEBUG) System.out.println("**** Visiting method " + jclass.getClassName() +  "." + mg.getName() + " ****");

			// Pre-screening hook for method.
			// The subclass may indicate that we don't need to build a CFG
			// for this method (because it is not interesting).
			if (!NOSCREEN && !preScreen(mg))
				continue;

			// Get the CFG for the method from the ClassContext
			CFG cfg = classContext.getCFG(method);

			if (PRINTCFG) {
				CFGPrinter printer = new CFGPrinter(cfg);
				printer.print(System.out);
			}

			visitCFG(cfg, mg);
		}

		finishClass();
	}

	/**
	 * Called when visiting the class for the first time,
	 * before constructing CFGs for methods.
	 * Subclasses may override.
	 * @param classContext the ClassContext for the class being visited
	 */
	public void startClass(ClassContext classContext) { }

	/**
	 * Called after all of the methods in the class have been visited.
	 * Subclasses may override.
	 */
	public void finishClass() { }

	/**
	 * Pre-screening hook for methods.
	 * Subclasses may override it to indicate methods that they aren't
	 * interested in examining.  The default implementation assumes
	 * that all methods should be analyzed.
	 *
	 * @return true if the subclass <em>does</em> want a CFG for the method,
	 *   false if it <em>does not want</em> a CFG for the method
	 */
	public boolean preScreen(MethodGen mg) {
		return true;
	}

	/** Return JavaClass currently being visited. */
	public JavaClass getJavaClass() {
		return classContext.getJavaClass();
	}

	/**
	 * Visit a method.
	 * @param cfg the method's control flow graph
	 * @param mg the method's MethodGen
	 */
	public abstract void visitCFG(CFG cfg, MethodGen mg);

	/**
	 * Convenience method for visiting every instruction in a CFG.
	 * If this method is used, visitInstruction() should be overridden.
	 */
	public void visitCFGInstructions(CFG cfg, MethodGen mg) {
			Iterator<BasicBlock> i = cfg.blockIterator();
			while (i.hasNext()) {
				BasicBlock bb = i.next();

				Iterator<InstructionHandle> j = bb.instructionIterator();
				while (j.hasNext()) {
					InstructionHandle handle = j.next();
					visitInstruction(handle, bb, mg);
				}
			}
	}

	/**
	 * Called by visitCFGInstructions() to visit each instruction in a CFG.
	 * @param handle the instruction to be visited
	 * @param bb the BasicBlock containing the instruction
	 * @param mg the MethodGen of the method being visited
	 */
	public void visitInstruction(InstructionHandle handle, BasicBlock bb, MethodGen mg) { }

	public void report() { }
}

// vim:ts=4
