/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ResourceCreationPoint;
import edu.umd.cs.findbugs.ResourceTrackingDetector;
import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

class Lock extends ResourceCreationPoint {
	private ValueNumber lockValue;

	public Lock(Location location, String lockClass, ValueNumber lockValue) {
		super(location, lockClass);
		this.lockValue = lockValue;
	}

	public ValueNumber getLockValue() {
		return lockValue;
	}
}

public class FindUnreleasedLock extends ResourceTrackingDetector<Lock, FindUnreleasedLock.LockResourceTracker> {

	private static final boolean DEBUG = Boolean.getBoolean("ful.debug");
	private static int numAcquires = 0;

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class LockFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
		private LockResourceTracker resourceTracker;
		private Lock lock;
		private ValueNumberDataflow vnaDataflow;

		public LockFrameModelingVisitor(ConstantPoolGen cpg, LockResourceTracker resourceTracker, Lock lock,
		                                ValueNumberDataflow vnaDataflow) {
			super(cpg);
			this.resourceTracker = resourceTracker;
			this.lock = lock;
			this.vnaDataflow = vnaDataflow;
		}

		public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) throws DataflowAnalysisException {
			final Instruction ins = handle.getInstruction();
			final ConstantPoolGen cpg = getCPG();
			final ResourceValueFrame frame = getFrame();

			int status = -1;

			// Is a lock acquired or released by this instruction?
			Location creationPoint = lock.getLocation();
			if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
				status = ResourceValueFrame.OPEN;
			} else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, lock, frame)) {
				status = ResourceValueFrame.CLOSED;
			}

			// Model use of instance values in frame slots
			ins.accept(this);

			final int updatedNumSlots = frame.getNumSlots();

			// Mark any appearances of the lock value in the ResourceValueFrame.
			ValueNumberFrame vnaFrame = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));
			if (DEBUG) System.out.println("vna frame after instruction: " + vnaFrame.toString());
			for (int i = 0; i < updatedNumSlots; ++i) {
				if (vnaFrame.getValue(i).equals(lock.getLockValue())) {
					if (DEBUG) System.out.println("Saw lock value!");
					frame.setValue(i, ResourceValue.instance());
				}
			}

			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
			}
		}

		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			return false;
		}
	}

	static class LockResourceTracker implements ResourceTracker<Lock> {
		private RepositoryLookupFailureCallback lookupFailureCallback;
		private ValueNumberDataflow vnaDataflow;

		public LockResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback, ValueNumberDataflow vnaDataflow) {
			this.lookupFailureCallback = lookupFailureCallback;
			this.vnaDataflow = vnaDataflow;
		}

		public Lock isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg)
		        throws DataflowAnalysisException {

			InvokeInstruction inv = toInvokeInstruction(handle.getInstruction());
			if (inv == null)
				return null;

			String className = inv.getClassName(cpg);
			String methodName = inv.getName(cpg);
			String methodSig = inv.getSignature(cpg);

			try {
				if (methodName.equals("lock") &&
				        methodSig.equals("()V") &&
				        Hierarchy.isSubtype(className, "java.util.concurrent.locks.Lock")) {

					Location location = new Location(handle, basicBlock);
					ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
					ValueNumber lockValue = frame.getTopValue();
					if (DEBUG) System.out.println("Lock value is " + lockValue.getNumber() + ", frame=" + frame.toString());
					if (DEBUG) ++numAcquires;
					return new Lock(location, className, lockValue);
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
			return null;
		}

		public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Lock resource,
		                               ResourceValueFrame frame) throws DataflowAnalysisException {

			InvokeInstruction inv = toInvokeInstruction(handle.getInstruction());
			if (inv == null)
				return false;

			String className = inv.getClassName(cpg);
			String methodName = inv.getName(cpg);
			String methodSig = inv.getSignature(cpg);

			ResourceValue topValue = frame.getTopValue();
			if (!topValue.isInstance())
				return false;

			try {
				if (methodName.equals("unlock") &&
				        methodSig.equals("()V") &&
				        Hierarchy.isSubtype(className, "java.util.concurrent.locks.Lock")) {
					return true;
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}

			return false;
		}

		public ResourceValueFrameModelingVisitor createVisitor(Lock resource, ConstantPoolGen cpg) {
			return new LockFrameModelingVisitor(cpg, this, resource, vnaDataflow);
		}

		public boolean ignoreImplicitExceptions(Lock resource) {
			// JSR166 locks should be ALWAYS be released,
			// including when implicit runtime exceptions are thrown
			return false;
		}

		public boolean isParamInstance(Lock resource, int slot) {
			// There is nothing special about Lock objects passed
			// into the method as parameters.
			return false;
		}

		private static final InvokeInstruction toInvokeInstruction(Instruction ins) {
			short opcode = ins.getOpcode();
			if (opcode != Constants.INVOKEVIRTUAL && opcode != Constants.INVOKEINTERFACE)
				return null;
			return (InvokeInstruction) ins;
		}
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindUnreleasedLock(BugReporter bugReporter) {
		super(bugReporter);
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.INVOKEVIRTUAL) || bytecodeSet.get(Constants.INVOKEINTERFACE);
	}

	public LockResourceTracker getResourceTracker(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return new LockResourceTracker(bugReporter, classContext.getValueNumberDataflow(method));
	}

	public void inspectResult(JavaClass javaClass, MethodGen methodGen, CFG cfg,
	                          Dataflow<ResourceValueFrame, ResourceValueAnalysis<Lock>> dataflow, Lock resource) {

		ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());
		int exitStatus = exitFrame.getStatus();

		if (exitStatus == ResourceValueFrame.OPEN || exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {
			String bugType;
			int priority;
			if (exitStatus == ResourceValueFrame.OPEN) {
				bugType = "UL_UNRELEASED_LOCK";
				priority = HIGH_PRIORITY;
			} else {
				bugType = "UL_UNRELEASED_LOCK_EXCEPTION_PATH";
				priority = NORMAL_PRIORITY;
			}

			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(this, bugType, priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addSourceLine(methodGen, sourceFile, resource.getLocation().getHandle()));
		}
	}

	public void report() {
		if (DEBUG) System.out.println("numAcquires=" + numAcquires);
	}

	/* ----------------------------------------------------------------------
	 * Test main() driver
	 * ---------------------------------------------------------------------- */

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + FindUnreleasedLock.class.getName() + " <class file> <method name> <bytecode offset>");
			System.exit(1);
		}

		String classFile = argv[0];
		String methodName = argv[1];
		int offset = Integer.parseInt(argv[2]);

		ResourceValueAnalysisTestDriver<Lock, LockResourceTracker> driver =
		        new ResourceValueAnalysisTestDriver<Lock, LockResourceTracker>() {
			        public LockResourceTracker createResourceTracker(ClassContext classContext, Method method)
			                throws CFGBuilderException, DataflowAnalysisException {

				        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
				        RepositoryLookupFailureCallback lookupFailureCallback = classContext.getLookupFailureCallback();

				        return new LockResourceTracker(lookupFailureCallback, vnaDataflow);
			        }
		        };

		driver.execute(classFile, methodName, offset);
	}
}

// vim:ts=4
