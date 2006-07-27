/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.npe.*;
import edu.umd.cs.findbugs.ba.vna.*;
import java.util.BitSet;
import org.apache.bcel.Constants;
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

public class FindUnreleasedLock extends ResourceTrackingDetector<Lock, FindUnreleasedLock.LockResourceTracker> {
	private static final boolean DEBUG = Boolean.getBoolean("ful.debug");
	private static int numAcquires = 0;
	
	private static final int JDK15_MAJOR = 48;
	private static final int JDK15_MINOR = 0;

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class LockFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
		private LockResourceTracker resourceTracker;
		private Lock lock;
		private ValueNumberDataflow vnaDataflow;
		//private IsNullValueDataflow isNullDataflow;

		public LockFrameModelingVisitor(
				ConstantPoolGen cpg,
				LockResourceTracker resourceTracker,
				Lock lock,
				ValueNumberDataflow vnaDataflow,
				IsNullValueDataflow isNullDataflow) {
			super(cpg);
			this.resourceTracker = resourceTracker;
			this.lock = lock;
			this.vnaDataflow = vnaDataflow;
			//this.isNullDataflow = isNullDataflow;
		}

		@Override
		public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) throws DataflowAnalysisException {
			final Instruction ins = handle.getInstruction();
			final ConstantPoolGen cpg = getCPG();
			final ResourceValueFrame frame = getFrame();

			int status = -1;

			if (DEBUG) System.out.println("PC : " + handle.getPosition() + " " + ins);
			if (DEBUG && ins instanceof InvokeInstruction) {
				InvokeInstruction iins = (InvokeInstruction) ins;
				System.out.println("  " + ins.toString(cpg.getConstantPool()));
			}
			if (DEBUG) System.out.println("resource frame before instruction: " + frame.toString());

			// Is a lock acquired or released by this instruction?
			Location creationPoint = lock.getLocation();
			if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
				status = ResourceValueFrame.OPEN;
				if (DEBUG) System.out.println("OPEN");
			} else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, lock, frame)) {
				status = ResourceValueFrame.CLOSED;
				if (DEBUG) System.out.println("CLOSE");
			}

			// Model use of instance values in frame slots
			analyzeInstruction(ins);

			final int updatedNumSlots = frame.getNumSlots();

			// Mark any appearances of the lock value in the ResourceValueFrame.
			ValueNumberFrame vnaFrame = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));
			if (DEBUG) {
				System.out.println("vna frame after instruction: " + vnaFrame.toString());
				System.out.println("Lock value number: " + lock.getLockValue());
				if (lock.getLockValue().hasFlag(ValueNumber.RETURN_VALUE) )
					System.out.println("is return value");
			}
			
			for (int i = 0; i < updatedNumSlots; ++i) {
				if (DEBUG) {
				System.out.println("Slot " + i);
				System.out.println("  Lock value number: " + vnaFrame.getValue(i));
				if (vnaFrame.getValue(i).hasFlag(ValueNumber.RETURN_VALUE) )
					System.out.println("  is return value");
				}
				if (lock.getLockValue().hasFlag(ValueNumber.RETURN_VALUE) 
						&& vnaFrame.getValue(i).hasFlag(ValueNumber.RETURN_VALUE) 
						|| vnaFrame.getValue(i).equals(lock.getLockValue())) {
					if (DEBUG) System.out.println("Saw lock value!");
					frame.setValue(i, ResourceValue.instance());
				}
			}
			
			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
			}
			if (DEBUG) System.out.println("resource frame after instruction: " + frame.toString());


		}

		@Override
		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			return false;
		}
	}

	static class LockResourceTracker implements ResourceTracker<Lock> {
		private RepositoryLookupFailureCallback lookupFailureCallback;
		private CFG cfg;
		private ValueNumberDataflow vnaDataflow;
		private IsNullValueDataflow isNullDataflow;

		public LockResourceTracker(
				RepositoryLookupFailureCallback lookupFailureCallback,
				CFG cfg,
				ValueNumberDataflow vnaDataflow,
				IsNullValueDataflow isNullDataflow) {
			this.lookupFailureCallback = lookupFailureCallback;
			this.cfg = cfg;
			this.vnaDataflow = vnaDataflow;
			this.isNullDataflow = isNullDataflow;
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
			return new LockFrameModelingVisitor(cpg, this, resource, vnaDataflow, isNullDataflow);
		}

		public boolean ignoreImplicitExceptions(Lock resource) {
			// JSR166 locks should be ALWAYS be released,
			// including when implicit runtime exceptions are thrown
			return false;
		}

		public boolean ignoreExceptionEdge(Edge edge, Lock resource, ConstantPoolGen cpg) {
			
			try {
				Location location = cfg.getExceptionThrowerLocation(edge);
				if (DEBUG) {
					System.out.println("Exception thrower location: " + location);
				}
				Instruction ins = location.getHandle().getInstruction();
				
				if (ins instanceof GETFIELD) {
					GETFIELD insGetfield = (GETFIELD)ins;
					String fieldName = insGetfield.getFieldName(cpg);
					if (DEBUG) {
						System.out.println("Inspecting GETFIELD of " + fieldName + " at " + location);
					}
					// Ignore exceptions from getfield instructions where the
					// object referece is known not to be null
					if (fieldName.equals("lock")) return true;
					IsNullValueFrame frame = isNullDataflow.getFactAtLocation(location);
					if (!frame.isValid())
						return false;
					IsNullValue receiver = frame.getInstance(ins, cpg);
					boolean notNull = receiver.isDefinitelyNotNull();
					if (DEBUG && notNull) {
						System.out.println("Ignoring exception from non-null GETFIELD");
					}
					return notNull;
				} else if (ins instanceof InvokeInstruction) {
					InvokeInstruction iins = (InvokeInstruction) ins;
					String methodName = iins.getMethodName(cpg);
					//System.out.println("Method " + methodName);
					if (methodName.equals("readLock") || methodName.equals("writeLock")) return true;
					if (methodName.equals("lock") || methodName.equals("unlock")) return true;
				}
			} catch (DataflowAnalysisException e) {
				// Report...
			}
			
			return false;
		}
		
		public boolean isParamInstance(Lock resource, int slot) {
			// There is nothing special about Lock objects passed
			// into the method as parameters.
			return false;
		}

		private static InvokeInstruction toInvokeInstruction(Instruction ins) {
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();
		
		// We can ignore classes that were compiled for anything
		// less than JDK 1.5.  This should avoid lots of unnecessary work
		// when analyzing code for older VM targets.
		
		if (jclass.getMajor() < JDK15_MAJOR ||
			(jclass.getMajor() == JDK15_MAJOR && jclass.getMinor() < JDK15_MINOR))  return;
		
		boolean  sawUtilConcurrentLocks = false;
		for(Constant c :  jclass.getConstantPool().getConstantPool()) 
			if (c instanceof ConstantMethodref) {
				ConstantMethodref m = (ConstantMethodref) c;
				ConstantClass cl = (ConstantClass) jclass.getConstantPool().getConstant(m.getClassIndex());
				ConstantUtf8 name =  (ConstantUtf8) jclass.getConstantPool().getConstant(cl.getNameIndex());
				String nameAsString = name.getBytes();
				if (nameAsString.startsWith("java/util/concurrent/locks")) sawUtilConcurrentLocks = true;
				
			}
		if (sawUtilConcurrentLocks) super.visitClassContext(classContext);
	}

	
	@Override
	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (bytecodeSet == null) return false;
		MethodGen methodGen = classContext.getMethodGen(method);
		return methodGen != null && methodGen.getName().toLowerCase().indexOf("lock") == -1
			&& (bytecodeSet.get(Constants.INVOKEVIRTUAL) 
				|| bytecodeSet.get(Constants.INVOKEINTERFACE));
	}

	@Override
	public LockResourceTracker getResourceTracker(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		return new LockResourceTracker(
				bugReporter,
				classContext.getCFG(method),
				classContext.getValueNumberDataflow(method),
				classContext.getIsNullValueDataflow(method));
	}

	@Override
	public void inspectResult(ClassContext classContext, MethodGen methodGen, CFG cfg,
			Dataflow<ResourceValueFrame, ResourceValueAnalysis<Lock>> dataflow, Lock resource) {

		JavaClass javaClass = classContext.getJavaClass();
		
		ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());
		if (DEBUG) {
			System.out.println("Resource value at exit: " + exitFrame);
		}
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
					.addSourceLine(classContext, methodGen, sourceFile, resource.getLocation().getHandle()));
		}
	}

	@Override
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
				@Override
				public LockResourceTracker createResourceTracker(ClassContext classContext, Method method)
						throws CFGBuilderException, DataflowAnalysisException {

					RepositoryLookupFailureCallback lookupFailureCallback = classContext.getLookupFailureCallback();

					return new LockResourceTracker(
								lookupFailureCallback,
								classContext.getCFG(method),
								classContext.getValueNumberDataflow(method),
								classContext.getIsNullValueDataflow(method));
				}
		};

		driver.execute(classFile, methodName, offset);
	}
}

// vim:ts=4
