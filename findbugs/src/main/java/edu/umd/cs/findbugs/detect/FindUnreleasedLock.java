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

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ResourceCreationPoint;
import edu.umd.cs.findbugs.ResourceTrackingDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.ResourceTracker;
import edu.umd.cs.findbugs.ba.ResourceValue;
import edu.umd.cs.findbugs.ba.ResourceValueAnalysis;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;
import edu.umd.cs.findbugs.ba.ResourceValueFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.bcel.BCELUtil;

class Lock extends ResourceCreationPoint {
    private final ValueNumber lockValue;

    public Lock(Location location, String lockClass, ValueNumber lockValue) {
        super(location, lockClass);
        this.lockValue = lockValue;
    }

    public ValueNumber getLockValue() {
        return lockValue;
    }
}

public class FindUnreleasedLock extends ResourceTrackingDetector<Lock, FindUnreleasedLock.LockResourceTracker> {
    private static final boolean DEBUG = SystemProperties.getBoolean("ful.debug");

    private int numAcquires = 0;

    private static class LockFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
        private final LockResourceTracker resourceTracker;

        private final Lock lock;

        private final ValueNumberDataflow vnaDataflow;

        // private IsNullValueDataflow isNullDataflow;

        public LockFrameModelingVisitor(ConstantPoolGen cpg, LockResourceTracker resourceTracker, Lock lock,
                ValueNumberDataflow vnaDataflow, IsNullValueDataflow isNullDataflow) {
            super(cpg);
            this.resourceTracker = resourceTracker;
            this.lock = lock;
            this.vnaDataflow = vnaDataflow;
            // this.isNullDataflow = isNullDataflow;
        }

        @Override
        public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) throws DataflowAnalysisException {
            final Instruction ins = handle.getInstruction();
            final ConstantPoolGen cpg = getCPG();
            final ResourceValueFrame frame = getFrame();

            int status = -1;

            if (DEBUG) {
                System.out.println("PC : " + handle.getPosition() + " " + ins);
            }
            if (DEBUG && ins instanceof InvokeInstruction) {
                InvokeInstruction iins = (InvokeInstruction) ins;
                System.out.println("  " + ins.toString(cpg.getConstantPool()));
            }
            if (DEBUG) {
                System.out.println("resource frame before instruction: " + frame.toString());
            }

            // Is a lock acquired or released by this instruction?
            Location creationPoint = lock.getLocation();
            if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
                status = ResourceValueFrame.OPEN;
                if (DEBUG) {
                    System.out.println("OPEN");
                }
            } else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, lock, frame)) {
                status = ResourceValueFrame.CLOSED;
                if (DEBUG) {
                    System.out.println("CLOSE");
                }
            }

            // Model use of instance values in frame slots
            analyzeInstruction(ins);

            final int updatedNumSlots = frame.getNumSlots();

            // Mark any appearances of the lock value in the ResourceValueFrame.
            ValueNumberFrame vnaFrame = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));
            if (DEBUG) {
                System.out.println("vna frame after instruction: " + vnaFrame.toString());
                System.out.println("Lock value number: " + lock.getLockValue());
                if (lock.getLockValue().hasFlag(ValueNumber.RETURN_VALUE)) {
                    System.out.println("is return value");
                }
            }

            for (int i = 0; i < updatedNumSlots; ++i) {
                if (DEBUG) {
                    System.out.println("Slot " + i);
                    System.out.println("  Lock value number: " + vnaFrame.getValue(i));
                    if (vnaFrame.getValue(i).hasFlag(ValueNumber.RETURN_VALUE)) {
                        System.out.println("  is return value");
                    }
                }
                if (vnaFrame.fuzzyMatch(lock.getLockValue(), vnaFrame.getValue(i))) {
                    if (DEBUG) {
                        System.out.println("Saw lock value!");
                    }
                    frame.setValue(i, ResourceValue.instance());
                }
            }

            // If needed, update frame status
            if (status != -1) {
                frame.setStatus(status);
            }
            if (DEBUG) {
                System.out.println("resource frame after instruction: " + frame.toString());
            }

        }

        @Override
        protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
            return false;
        }
    }

    class LockResourceTracker implements ResourceTracker<Lock> {
        private final RepositoryLookupFailureCallback lookupFailureCallback;

        private final CFG cfg;

        private final ValueNumberDataflow vnaDataflow;

        private final IsNullValueDataflow isNullDataflow;

        public LockResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback, CFG cfg,
                ValueNumberDataflow vnaDataflow, IsNullValueDataflow isNullDataflow) {
            this.lookupFailureCallback = lookupFailureCallback;
            this.cfg = cfg;
            this.vnaDataflow = vnaDataflow;
            this.isNullDataflow = isNullDataflow;
        }

        @Override
        public Lock isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg)
                throws DataflowAnalysisException {

            InvokeInstruction inv = toInvokeInstruction(handle.getInstruction());
            if (inv == null) {
                return null;
            }

            String className = inv.getClassName(cpg);
            String methodName = inv.getName(cpg);
            String methodSig = inv.getSignature(cpg);

            try {
                if ("lock".equals(methodName) && "()V".equals(methodSig)
                        && Hierarchy.isSubtype(className, "java.util.concurrent.locks.Lock")) {

                    Location location = new Location(handle, basicBlock);
                    ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
                    ValueNumber lockValue = frame.getTopValue();
                    if (DEBUG) {
                        System.out.println("Lock value is " + lockValue.getNumber() + ", frame=" + frame.toString());
                    }
                    if (DEBUG) {
                        ++numAcquires;
                    }
                    return new Lock(location, className, lockValue);
                }
            } catch (ClassNotFoundException e) {
                lookupFailureCallback.reportMissingClass(e);
            }
            return null;
        }

        @Override
        public boolean mightCloseResource(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg)
                throws DataflowAnalysisException {
            InvokeInstruction inv = toInvokeInstruction(handle.getInstruction());
            if (inv == null) {
                return false;
            }

            String className = inv.getClassName(cpg);
            String methodName = inv.getName(cpg);
            String methodSig = inv.getSignature(cpg);

            try {
                if ("unlock".equals(methodName) && "()V".equals(methodSig)
                        && Hierarchy.isSubtype(className, "java.util.concurrent.locks.Lock")) {

                    return true;
                }
            } catch (ClassNotFoundException e) {
                lookupFailureCallback.reportMissingClass(e);
            }

            return false;
        }

        @Override
        public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Lock resource,
                ResourceValueFrame frame) throws DataflowAnalysisException {

            if (!mightCloseResource(basicBlock, handle, cpg)) {
                return false;
            }
            ResourceValue topValue = frame.getTopValue();
            return topValue.isInstance();

        }

        @Override
        public ResourceValueFrameModelingVisitor createVisitor(Lock resource, ConstantPoolGen cpg) {
            return new LockFrameModelingVisitor(cpg, this, resource, vnaDataflow, isNullDataflow);
        }

        @Override
        public boolean ignoreImplicitExceptions(Lock resource) {
            // JSR166 locks should be ALWAYS be released,
            // including when implicit runtime exceptions are thrown
            return false;
        }

        @Override
        public boolean ignoreExceptionEdge(Edge edge, Lock resource, ConstantPoolGen cpg) {

            try {
                Location location = cfg.getExceptionThrowerLocation(edge);
                if (DEBUG) {
                    System.out.println("Exception thrower location: " + location);
                }
                Instruction ins = location.getHandle().getInstruction();

                if (ins instanceof GETFIELD) {
                    GETFIELD insGetfield = (GETFIELD) ins;
                    String fieldName = insGetfield.getFieldName(cpg);
                    if (DEBUG) {
                        System.out.println("Inspecting GETFIELD of " + fieldName + " at " + location);
                    }
                    // Ignore exceptions from getfield instructions where the
                    // object reference is known not to be null
                    if ("lock".equals(fieldName)) {
                        return true;
                    }
                    IsNullValueFrame frame = isNullDataflow.getFactAtLocation(location);
                    if (!frame.isValid()) {
                        return false;
                    }
                    IsNullValue receiver = frame.getInstance(ins, cpg);
                    boolean notNull = receiver.isDefinitelyNotNull();
                    if (DEBUG && notNull) {
                        System.out.println("Ignoring exception from non-null GETFIELD");
                    }
                    return notNull;
                } else if (ins instanceof InvokeInstruction) {
                    InvokeInstruction iins = (InvokeInstruction) ins;
                    String methodName = iins.getMethodName(cpg);
                    // System.out.println("Method " + methodName);
                    if (methodName.startsWith("access$")) {
                        return true;
                    }
                    if ("readLock".equals(methodName) || "writeLock".equals(methodName)) {
                        return true;
                    }
                    if ("lock".equals(methodName) || "unlock".equals(methodName)) {
                        return true;
                    }
                }
                if (DEBUG) {
                    System.out.println("FOUND Exception thrower at: " + location);
                }
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("Error while looking for exception edge", e);
            }

            return false;
        }

        @Override
        public boolean isParamInstance(Lock resource, int slot) {
            // There is nothing special about Lock objects passed
            // into the method as parameters.
            return false;
        }

        private InvokeInstruction toInvokeInstruction(Instruction ins) {
            short opcode = ins.getOpcode();
            if (opcode != Constants.INVOKEVIRTUAL && opcode != Constants.INVOKEINTERFACE) {
                return null;
            }
            return (InvokeInstruction) ins;
        }
    }

    /*
     * ----------------------------------------------------------------------
     * Implementation
     * ----------------------------------------------------------------------
     */

    public FindUnreleasedLock(BugReporter bugReporter) {
        super(bugReporter);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba
     * .ClassContext)
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass jclass = classContext.getJavaClass();

        // We can ignore classes that were compiled for anything
        // less than JDK 1.5. This should avoid lots of unnecessary work
        // when analyzing code for older VM targets.
        if (BCELUtil.preTiger(jclass)) {
            return;
        }

        boolean sawUtilConcurrentLocks = false;
        for (Constant c : jclass.getConstantPool().getConstantPool()) {
            if (c instanceof ConstantMethodref) {
                ConstantMethodref m = (ConstantMethodref) c;
                ConstantClass cl = (ConstantClass) jclass.getConstantPool().getConstant(m.getClassIndex());
                ConstantUtf8 name = (ConstantUtf8) jclass.getConstantPool().getConstant(cl.getNameIndex());
                String nameAsString = name.getBytes();
                if (nameAsString.startsWith("java/util/concurrent/locks")) {
                    sawUtilConcurrentLocks = true;
                }

            }
        }
        if (sawUtilConcurrentLocks) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public boolean prescreen(ClassContext classContext, Method method, boolean mightClose) {
        if (!mightClose) {
            return false;
        }
        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        if (bytecodeSet == null) {
            return false;
        }

        MethodGen methodGen = classContext.getMethodGen(method);

        return methodGen != null && methodGen.getName().toLowerCase().indexOf("lock") == -1
                && (bytecodeSet.get(Constants.INVOKEVIRTUAL) || bytecodeSet.get(Constants.INVOKEINTERFACE));
    }

    @Override
    public LockResourceTracker getResourceTracker(ClassContext classContext, Method method) throws CFGBuilderException,
    DataflowAnalysisException {
        return new LockResourceTracker(bugReporter, classContext.getCFG(method), classContext.getValueNumberDataflow(method),
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
            Location location = resource.getLocation();
            InstructionHandle handle = location.getHandle();
            InstructionHandle nextInstruction = handle.getNext();
            if (nextInstruction.getInstruction() instanceof RETURN)
            {
                return; // don't report as error; intentional
            }
            bugAccumulator.accumulateBug(new BugInstance(this, bugType, priority).addClassAndMethod(methodGen, sourceFile),
                    SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle));
        }
    }

    @Override
    public void report() {
        if (DEBUG) {
            System.out.println("numAcquires=" + numAcquires);
        }
    }

    // /* ----------------------------------------------------------------------
    // * Test main() driver
    // * ----------------------------------------------------------------------
    // */
    //
    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 3) {
    // System.err.println("Usage: " + FindUnreleasedLock.class.getName() +
    // " <class file> <method name> <bytecode offset>");
    // System.exit(1);
    // }
    //
    // String classFile = argv[0];
    // String methodName = argv[1];
    // int offset = Integer.parseInt(argv[2]);
    // final FindUnreleasedLock detector = new FindUnreleasedLock(null);
    //
    // ResourceValueAnalysisTestDriver<Lock, LockResourceTracker> driver =
    // new ResourceValueAnalysisTestDriver<Lock, LockResourceTracker>() {
    // @Override
    // public LockResourceTracker createResourceTracker(ClassContext
    // classContext, Method method)
    // throws CFGBuilderException, DataflowAnalysisException {
    //
    // RepositoryLookupFailureCallback lookupFailureCallback =
    // classContext.getLookupFailureCallback();
    //
    // return detector.new LockResourceTracker(
    // lookupFailureCallback,
    // classContext.getCFG(method),
    // classContext.getValueNumberDataflow(method),
    // classContext.getIsNullValueDataflow(method));
    // }
    // };
    //
    // driver.execute(classFile, methodName, offset);
    // }
}

