/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;

public class FindDeadLocalStores implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("fdls.debug");

	/**
	 * Opcodes of instructions that load constant values that
	 * often indicate defensive programming.
	 */
	private static final BitSet defensiveConstantValueOpcodes = new BitSet();
	static {
		defensiveConstantValueOpcodes.set(Constants.ACONST_NULL);
		defensiveConstantValueOpcodes.set(Constants.ICONST_0);
	}

	private BugReporter bugReporter;

	public FindDeadLocalStores(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
	}

	private boolean prescreen(ClassContext classContext, Method method) {
		return true;
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];

			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			if (!prescreen(classContext, method))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			} catch (CFGBuilderException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
		throws DataflowAnalysisException, CFGBuilderException {

		JavaClass javaClass = classContext.getJavaClass();

		Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow =
			classContext.getLiveLocalStoreDataflow(method);

		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			if (!isStore(location))
				continue;

			// Ignore exception handler blocks:
			// javac always stores caught exceptions in
			// a local, even if the value is not used.
			if (location.getBasicBlock().isExceptionHandler())
				continue;

			IndexedInstruction store = (IndexedInstruction) location.getHandle().getInstruction();
			int local = store.getIndex();

			BitSet liveStoreSet = llsaDataflow.getAnalysis().getFactAtLocation(location);

			if (liveStoreSet.get(local))
				continue;

			// Store is dead

			// Ignore dead assignments of null and 0.
			// These often indicate defensive programming.
			InstructionHandle prev = location.getBasicBlock().getPredecessorOf(location.getHandle());
			if (prev != null && defensiveConstantValueOpcodes.get(prev.getInstruction().getOpcode()))
				continue;

			BugInstance bugInstance = new BugInstance("DLS_DEAD_LOCAL_STORE", NORMAL_PRIORITY)
				.addClassAndMethod(methodGen, javaClass.getSourceFileName())
				.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle());

			bugReporter.reportBug(bugInstance);
		}
	}

	private boolean isStore(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof StoreInstruction) || (ins instanceof IINC);
	}

	public void report() {
	}
}

// vim:ts=4
