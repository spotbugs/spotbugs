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
	public Lock(Location location, String lockClass) {
		super(location, lockClass);
	}
}

public class FindUnreleasedLock extends ResourceTrackingDetector<Lock> {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class LockFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
		private LockResourceTracker resourceTracker;
		private Lock lock;

		public LockFrameModelingVisitor(ConstantPoolGen cpg, LockResourceTracker resourceTracker, Lock lock) {
			super(cpg);
			this.resourceTracker = resourceTracker;
			this.lock = lock;
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

			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
				if (status == ResourceValueFrame.CREATED)
					frame.setValue(frame.getNumSlots() - 1, ResourceValue.instance());
			}
		}

		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			return false;
		}
	}

	private static class LockResourceTracker implements ResourceTracker<Lock> {
		private RepositoryLookupFailureCallback lookupFailureCallback;

		public LockResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback) {
			this.lookupFailureCallback = lookupFailureCallback;
		}

		public Lock isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {
			Instruction ins = handle.getInstruction();
			if (!(ins instanceof INVOKEVIRTUAL))
				return null;
			INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

			String className = inv.getClassName(cpg);
			String methodName = inv.getName(cpg);
			String methodSig = inv.getSignature(cpg);

			try {
				if (Repository.instanceOf(className, "java.util.concurrent.Lock") &&
					methodName.equals("lock") &&
					methodSig.equals("()V")) {
					return new Lock(new Location(handle, basicBlock), className);
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
			return null;
		}

		public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Lock resource,
			ResourceValueFrame frame) throws DataflowAnalysisException {
			Instruction ins = handle.getInstruction();
			if (!(ins instanceof INVOKEVIRTUAL))
				return false;
			INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

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
			return new LockFrameModelingVisitor(cpg, this, resource);
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindUnreleasedLock(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.NEW) && bytecodeSet.get(Constants.INVOKEVIRTUAL);
	}

	public ResourceTracker<Lock> getResourceTracker() {
		return new LockResourceTracker(bugReporter);
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
}

// vim:ts=4
