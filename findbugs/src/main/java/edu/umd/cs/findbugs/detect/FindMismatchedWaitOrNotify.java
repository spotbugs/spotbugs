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
import java.util.Collection;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public final class FindMismatchedWaitOrNotify implements Detector, StatelessDetector {
    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    public FindMismatchedWaitOrNotify(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass jclass = classContext.getJavaClass();

        Method[] methodList = jclass.getMethods();
        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
                continue;
            }

            // Don't bother analyzing the method unless there is both locking
            // and a method call.
            BitSet bytecodeSet = classContext.getBytecodeSet(method);
            if (bytecodeSet == null) {
                continue;
            }
            if (!(bytecodeSet.get(Constants.MONITORENTER) && bytecodeSet.get(Constants.INVOKEVIRTUAL))) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("FindMismatchedWaitOrNotify: caught exception", e);
            } catch (CFGBuilderException e) {
                bugReporter.logError("FindMismatchedWaitOrNotify: caught exception", e);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {

        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }
        ConstantPoolGen cpg = methodGen.getConstantPool();
        CFG cfg = classContext.getCFG(method);
        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
        LockDataflow dataflow = classContext.getLockDataflow(method);

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            InstructionHandle handle = location.getHandle();

            Instruction ins = handle.getInstruction();
            if (!(ins instanceof INVOKEVIRTUAL)) {
                continue;
            }
            INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

            String methodName = inv.getName(cpg);
            String methodSig = inv.getSignature(cpg);

            if (Hierarchy.isMonitorWait(methodName, methodSig) || Hierarchy.isMonitorNotify(methodName, methodSig)) {
                int numConsumed = inv.consumeStack(cpg);
                if (numConsumed == Constants.UNPREDICTABLE) {
                    throw new DataflowAnalysisException("Unpredictable stack consumption", methodGen, handle);
                }

                ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
                if (!frame.isValid()) {
                    // Probably dead code
                    continue;
                }
                if (frame.getStackDepth() - numConsumed < 0) {
                    throw new DataflowAnalysisException("Stack underflow", methodGen, handle);
                }
                ValueNumber ref = frame.getValue(frame.getNumSlots() - numConsumed);
                LockSet lockSet = dataflow.getFactAtLocation(location);
                int lockCount = lockSet.getLockCount(ref.getNumber());

                if (lockCount == 0) {
                    Collection<ValueNumber> lockedValueNumbers = lockSet.getLockedValueNumbers(frame);
                    boolean foundMatch = false;
                    for (ValueNumber v : lockedValueNumbers) {
                        if (frame.veryFuzzyMatch(ref, v)) {
                            foundMatch = true;
                            break;
                        }
                    }

                    if (!foundMatch) {

                        String type = "wait".equals(methodName) ? "MWN_MISMATCHED_WAIT" : "MWN_MISMATCHED_NOTIFY";
                        String sourceFile = classContext.getJavaClass().getSourceFileName();
                        // Report as medium priority only if the method is
                        // public.
                        // Non-public methods may be properly locked in a
                        // calling context.
                        int priority = method.isPublic() ? NORMAL_PRIORITY : LOW_PRIORITY;

                        bugAccumulator.accumulateBug(
                                new BugInstance(this, type, priority).addClassAndMethod(methodGen, sourceFile),
                                SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle));
                    }
                }
            }
        }
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void report() {
    }
}

