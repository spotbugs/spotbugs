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

import java.util.HashSet;
import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.LoadInstruction;

public class FindDeadLocalStores implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("fdls.debug");

	private static final HashSet classesAlreadyReportedOn = new HashSet();
	/**
	 * Opcodes of instructions that load constant values that
	 * often indicate defensive programming.
	 */
	private static final BitSet defensiveConstantValueOpcodes = new BitSet();
	static {
		defensiveConstantValueOpcodes.set(Constants.DCONST_0);
		defensiveConstantValueOpcodes.set(Constants.DCONST_1);
		defensiveConstantValueOpcodes.set(Constants.FCONST_0);
		defensiveConstantValueOpcodes.set(Constants.FCONST_1);
		defensiveConstantValueOpcodes.set(Constants.ACONST_NULL);
		defensiveConstantValueOpcodes.set(Constants.ICONST_0);
		defensiveConstantValueOpcodes.set(Constants.ICONST_1);
	}

	private BugReporter bugReporter;

	public FindDeadLocalStores(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		if (DEBUG) System.out.println("Debugging FindDeadLocalStores detector");
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

		int numLocals = method.getCode().getMaxLocals();
		int [] localUpdateCount = new int[numLocals];
		int [] localLoadCount = new int[numLocals];
		int [] localIncrementCount = new int[numLocals];
		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);
		BitSet liveStoreSetAtEntry = llsaDataflow.getAnalysis().getResultFact(cfg.getEntry());
		BitSet complainedAbout = new BitSet();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			if (location.getBasicBlock().isExceptionHandler())
				continue;

			boolean isStore = isStore(location);
			boolean isLoad = isLoad(location);
			if (!isStore && !isLoad) continue;
			IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
			int local = ins.getIndex();
			if (ins instanceof IINC) {
				localUpdateCount[local]++;
				localLoadCount[local]++;
				localIncrementCount[local]++;
			}
			else if (isStore) 
				localUpdateCount[local]++;
			else 
				localLoadCount[local]++;
			}


		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			if (!isStore(location))
				continue;

			// Ignore exception handler blocks:
			// javac always stores caught exceptions in
			// a local, even if the value is not used.
			if (location.getBasicBlock().isExceptionHandler())
				continue;

			IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
			int local = ins.getIndex();
			int localsThatAreParameters = method.getArgumentTypes().length;
			if (!method.isStatic()) localsThatAreParameters++;

			boolean parameterThatIsDeadAtEntry = local < localsThatAreParameters
			    && !llsaDataflow.getAnalysis().isStoreAlive(liveStoreSetAtEntry, local);
			if (parameterThatIsDeadAtEntry && !complainedAbout.get(local)) {
			BugInstance bugInstance = new BugInstance(this, "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", NORMAL_PRIORITY)
				.addClassAndMethod(methodGen, javaClass.getSourceFileName())
				.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle());
			bugReporter.reportBug(bugInstance);
			complainedAbout.set(local);
			}

			// Get live stores at this instruction.
			// Note that the analysis also computes which stores were
			// killed by a subsequent unconditional store.
			BitSet liveStoreSet = llsaDataflow.getAnalysis().getFactAtLocation(location);

			// Is store alive?
			if (llsaDataflow.getAnalysis().isStoreAlive(liveStoreSet, local))
				continue;

			// Store is dead

			

			// Ignore assignments that were killed by a subsequent assignment.
			if (llsaDataflow.getAnalysis().killedByStore(liveStoreSet, local)) {
				if (DEBUG) {
					System.out.println("Store at " + location.getHandle() +
						" killed by subsequent store");
				}
				continue;
			}

			// Ignore dead assignments of null and 0.
			// These often indicate defensive programming.
			InstructionHandle prev = location.getBasicBlock().getPredecessorOf(location.getHandle());
			if (prev != null && defensiveConstantValueOpcodes.get(prev.getInstruction().getOpcode()))
				continue;

			int priority = LOW_PRIORITY;
			// special handling of IINC
			if (ins instanceof IINC) {
				
				 if (localIncrementCount[local] == 1) 
					priority = NORMAL_PRIORITY;
				else continue;

				}

			else if (ins instanceof ASTORE
				&& prev != null) { 
				Instruction prevIns = prev.getInstruction();
				if ((prevIns instanceof INVOKESPECIAL &&
					((INVOKESPECIAL)prevIns).getMethodName(methodGen.getConstantPool()).equals("<init>"))
				    || prevIns instanceof ANEWARRAY
				    || prevIns instanceof NEWARRAY
				    || prevIns instanceof MULTIANEWARRAY)
					priority--;
			}


			else if (localUpdateCount[local] == 2
				&& localLoadCount[local] > 0) priority--; // raise priority
			else if (localUpdateCount[local] == 1)
				priority++;

			else if (localLoadCount[local] == 0) priority++; // lower priority
			if (parameterThatIsDeadAtEntry) {
				if (DEBUG) System.out.println("Raising priority");
				priority--;
				}

			if (priority < HIGH_PRIORITY)
				priority =  HIGH_PRIORITY;
			if (DEBUG) System.out.println(" considering reporting bug:" + priority);
			if (priority != LOW_PRIORITY 
				|| classesAlreadyReportedOn.add(javaClass.getClassName())) {

			// Report the bug
			BugInstance bugInstance = new BugInstance(this, "DLS_DEAD_LOCAL_STORE", priority)
				.addClassAndMethod(methodGen, javaClass.getSourceFileName())
				.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle());
			if (DEBUG) System.out.println("Reporting " + bugInstance);

			bugReporter.reportBug(bugInstance);
			}
		}
	}

	private boolean isStore(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof StoreInstruction) || (ins instanceof IINC);
	}
	private boolean isLoad(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof LoadInstruction) || (ins instanceof IINC);
	}

	public void report() {
	}
}

// vim:ts=4
