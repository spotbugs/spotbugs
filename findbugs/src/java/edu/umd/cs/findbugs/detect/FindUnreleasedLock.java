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

package edu.umd.cs.findbugs.detect;

import java.util.*;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.daveho.ba.*;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
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

public class FindUnreleasedLock extends ResourceTrackingDetector<Lock> {

	private static final boolean DEBUG = Boolean.getBoolean("ful.debug");

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
			final ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
			final int origNumSlots = frame.getNumSlots();

			// Mark any appearances of the lock value in the ResourceValueFrame.
			// A lock value could appear "spontaneously" if, for example, the lock
			// was in a final field.  (We reuse the same value for loads of final
			// fields - see ValueNumberAnalysis.  FIXME: Actually, at the moment we reuse
			// the same value number for loads of non-final fields, but that's just a bug :-)
			if (DEBUG) System.out.println("Incoming vna frame: " + vnaFrame.toString());
			for (int i = 0; i < origNumSlots; ++i) {
				if (vnaFrame.getValue(i).equals(lock.getLockValue())) {
					if (DEBUG) System.out.println("Saw lock value!");
					frame.setValue(i, ResourceValue.instance());
				}
			}

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

			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
				if (status == ResourceValueFrame.OPEN) {
					// Look in the value number frame to see which slots have
					// the same value as the Lock object.  Mark those slots
					// in the resource value frame as containing the resource instance.

					// Note: this only works if the resource
					//   - was in a local and was ALOAD'ed, or
					//   - was on the stack and was DUP'ed

					ValueNumber instanceValueNumber = vnaFrame.getTopValue();
					final int updatedNumSlots = frame.getNumSlots();

					for (int i = 0; i < updatedNumSlots; ++i) {
						if (vnaFrame.getValue(i).equals(instanceValueNumber))
							frame.setValue(i, ResourceValue.instance());
					}
				}
			}
		}

		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			return false;
		}
	}

	private static class LockResourceTracker implements ResourceTracker<Lock> {
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
				if (Repository.instanceOf(className, "java.util.concurrent.Lock") &&
					methodName.equals("lock") &&
					methodSig.equals("()V")) {

					Location location = new Location(handle, basicBlock);
					ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
					ValueNumber lockValue = frame.getTopValue();
					if (DEBUG) System.out.println("Lock value is " + lockValue.getNumber() + ", frame=" + frame.toString());
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
				if (Repository.instanceOf(className, "java.util.concurrent.Lock") &&
					methodName.equals("unlock") &&
					methodSig.equals("()V")) {
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

	public ResourceTracker<Lock> getResourceTracker(ClassContext classContext, Method method)
		throws CFGBuilderException, DataflowAnalysisException {
		return new LockResourceTracker(bugReporter, classContext.getValueNumberDataflow(method));
	}

	public void inspectResult(JavaClass javaClass, MethodGen methodGen, CFG cfg,
		Dataflow<ResourceValueFrame> dataflow, Lock resource) {

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
			bugReporter.reportBug(new BugInstance(bugType, priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, resource.getLocation().getHandle())
			);
		}
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

		ResourceValueAnalysisTestDriver<Lock> driver = new ResourceValueAnalysisTestDriver<Lock>() {
			public ResourceTracker<Lock> createResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback,
				MethodGen methodGen, CFG cfg) throws CFGBuilderException, DataflowAnalysisException {
				ValueNumberAnalysis vna = new ValueNumberAnalysis(methodGen);
				ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, vna);
				vnaDataflow.execute();
				return new LockResourceTracker(lookupFailureCallback, vnaDataflow);
			}
		};

		driver.execute(classFile, methodName, offset);
	}
}

// vim:ts=4
