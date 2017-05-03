/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2008 University of Maryland
 * Copyright (C) 2008 Google
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

import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.bcel.BCELUtil;

public class CallToUnsupportedMethod implements Detector {

    BugReporter bugReporter;

    public CallToUnsupportedMethod(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (MethodUnprofitableException e) {
                assert true; // move along; nothing to see
            } catch (CFGBuilderException e) {
                String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
                        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
                bugReporter.logError(msg, e);
            } catch (DataflowAnalysisException e) {
                String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
                        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
                bugReporter.logError(msg, e);
            }
        }
    }

    /**
     * @param classContext
     * @param method
     */
    private void analyzeMethod(ClassContext classContext, Method method) throws MethodUnprofitableException, CFGBuilderException,
    DataflowAnalysisException {
        if (BCELUtil.isSynthetic(method)|| (method.getAccessFlags() & Const.ACC_BRIDGE) == Const.ACC_BRIDGE) {
            return;
        }
        CFG cfg = classContext.getCFG(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        ConstantPoolGen constantPoolGen = classContext.getConstantPoolGen();

        locationLoop: for (Iterator<Location> iter = cfg.locationIterator(); iter.hasNext();) {
            Location location = iter.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            // Only consider invoke instructions
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }
            if (ins instanceof INVOKEINTERFACE) {
                continue;
            }

            InvokeInstruction inv = (InvokeInstruction) ins;
            TypeFrame frame = typeDataflow.getFactAtLocation(location);

            String methodName = inv.getMethodName(constantPoolGen);
            if (methodName.toLowerCase().indexOf("unsupported") >= 0) {
                continue;
            }
            String methodSig = inv.getSignature(constantPoolGen);
            if ("()Ljava/lang/UnsupportedOperationException;".equals(methodSig)) {
                continue;
            }

            Set<XMethod> targets;
            try {

                targets = Hierarchy2.resolveMethodCallTargets(inv, frame, constantPoolGen);
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
                continue locationLoop;
            }
            if (targets.isEmpty()) {
                continue locationLoop;
            }
            int priority = targets.size() == 1 ? Priorities.HIGH_PRIORITY : Priorities.NORMAL_PRIORITY;
            for (XMethod m : targets) {
                if (!m.isUnsupported()) {
                    continue locationLoop;
                }
                XClass xc = AnalysisContext.currentXFactory().getXClass(m.getClassDescriptor());
                if (!(inv instanceof INVOKESTATIC) && !(m.isFinal() || xc.isFinal())) {
                    priority = Priorities.NORMAL_PRIORITY;
                }
                if (xc == null || xc.isAbstract()) {
                    try {
                        if (!AnalysisContext.currentAnalysisContext().getSubtypes2().hasSubtypes(m.getClassDescriptor())) {
                            continue locationLoop;
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                        continue locationLoop;
                    }
                }
            }
            BugInstance bug = new BugInstance(this, "DMI_UNSUPPORTED_METHOD", priority)
            .addClassAndMethod(classContext.getJavaClass(), method).addCalledMethod(constantPoolGen, inv)
            .addSourceLine(classContext, method, location);
            bugReporter.reportBug(bug);

        }

    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}
