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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MethodGen;

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

public final class FindTwoLockWait implements Detector, StatelessDetector {

    private final BugReporter bugReporter;

    private JavaClass javaClass;

    private final Collection<BugInstance> possibleWaitBugs = new LinkedList<BugInstance>();

    private final Collection<SourceLineAnnotation> possibleNotifyLocations = new LinkedList<SourceLineAnnotation>();

    public FindTwoLockWait(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
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
        javaClass = classContext.getJavaClass();
        possibleWaitBugs.clear();
        possibleNotifyLocations.clear();
        Method[] methodList = javaClass.getMethods();
        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
                continue;
            }

            if (!preScreen(methodGen)) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (DataflowAnalysisException e) {
                // bugReporter.logError("Error analyzing " + method.toString(),
                // e);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Error analyzing " + method.toString(), e);
            }
        }
        if (!possibleNotifyLocations.isEmpty()) {
            for (BugInstance bug : possibleWaitBugs) {
                for (SourceLineAnnotation notifyLine : possibleNotifyLocations) {
                    bug.addSourceLine(notifyLine).describe("SOURCE_NOTIFICATION_DEADLOCK");
                }
                bugReporter.reportBug(bug);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {

        MethodGen methodGen = classContext.getMethodGen(method);
        CFG cfg = classContext.getCFG(method);
        LockDataflow dataflow = classContext.getLockDataflow(method);

        for (Iterator<Location> j = cfg.locationIterator(); j.hasNext();) {
            Location location = j.next();
            visitLocation(classContext, location, methodGen, dataflow);
        }
    }

    public boolean preScreen(MethodGen mg) {
        ConstantPoolGen cpg = mg.getConstantPool();

        int lockCount = mg.isSynchronized() ? 1 : 0;
        boolean sawWaitOrNotify = false;

        InstructionHandle handle = mg.getInstructionList().getStart();
        while (handle != null && !(lockCount >= 2 && sawWaitOrNotify)) {
            Instruction ins = handle.getInstruction();
            if (ins instanceof MONITORENTER) {
                ++lockCount;
            } else if (ins instanceof INVOKEVIRTUAL) {
                INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
                String methodName = inv.getMethodName(cpg);
                if ("wait".equals(methodName) || methodName.startsWith("notify")) {
                    sawWaitOrNotify = true;
                }
            }

            handle = handle.getNext();
        }

        return lockCount >= 2 && sawWaitOrNotify;
    }

    public void visitLocation(ClassContext classContext, Location location, MethodGen methodGen, LockDataflow dataflow)
            throws DataflowAnalysisException {
        ConstantPoolGen cpg = methodGen.getConstantPool();

        if (Hierarchy.isMonitorWait(location.getHandle().getInstruction(), cpg)) {
            int count = dataflow.getFactAtLocation(location).getNumLockedObjects();
            if (count > 1) {
                // A wait with multiple locks held?
                String sourceFile = javaClass.getSourceFileName();
                possibleWaitBugs.add(new BugInstance(this, "TLW_TWO_LOCK_WAIT", HIGH_PRIORITY).addClassAndMethod(methodGen,
                        sourceFile).addSourceLine(classContext, methodGen, sourceFile, location.getHandle()));
            }
        }
        if (Hierarchy.isMonitorNotify(location.getHandle().getInstruction(), cpg)) {
            int count = dataflow.getFactAtLocation(location).getNumLockedObjects();
            if (count > 1) {
                // A notify with multiple locks held?
                String sourceFile = javaClass.getSourceFileName();
                possibleNotifyLocations.add(SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile,
                        location.getHandle()));
            }
        }
    }

    @Override
    public void report() {
    }
}

