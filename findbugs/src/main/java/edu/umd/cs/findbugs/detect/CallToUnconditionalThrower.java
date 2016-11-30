/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class CallToUnconditionalThrower extends PreorderVisitor implements Detector {

    static boolean DEBUG = false;

    BugReporter bugReporter;

    AnalysisContext analysisContext;

    private final boolean testingEnabled;

    public CallToUnconditionalThrower(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    @Override
    public void report() {
        //
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        if (BCELUtil.isSynthetic(method) || (method.getAccessFlags() & Constants.ACC_BRIDGE) == Constants.ACC_BRIDGE) {
            return;
        }
        CFG cfg = classContext.getCFG(method);

        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

        for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
            BasicBlock basicBlock = i.next();

            // Check if it's a method invocation.
            if (!basicBlock.isExceptionThrower()) {
                continue;
            }
            InstructionHandle thrower = basicBlock.getExceptionThrower();
            Instruction ins = thrower.getInstruction();
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }

            InvokeInstruction inv = (InvokeInstruction) ins;
            boolean foundThrower = false;
            boolean foundNonThrower = false;

            if (inv instanceof INVOKEINTERFACE) {
                continue;
            }

            String className = inv.getClassName(cpg);

            Location loc = new Location(thrower, basicBlock);
            TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
            XMethod primaryXMethod = XFactory.createXMethod(inv, cpg);
            // if (primaryXMethod.isAbstract()) continue;
            Set<XMethod> targetSet = null;
            try {

                if (className.startsWith("[")) {
                    continue;
                }
                String methodSig = inv.getSignature(cpg);
                if (!methodSig.endsWith("V")) {
                    continue;
                }

                targetSet = Hierarchy2.resolveMethodCallTargets(inv, typeFrame, cpg);

                for (XMethod xMethod : targetSet) {
                    if (DEBUG) {
                        System.out.println("\tFound " + xMethod);
                    }

                    boolean isUnconditionalThrower = xMethod.isUnconditionalThrower() && !xMethod.isUnsupported()
                            && !xMethod.isSynthetic();
                    if (isUnconditionalThrower) {
                        foundThrower = true;
                        if (DEBUG) {
                            System.out.println("Found thrower");
                        }
                    } else {
                        foundNonThrower = true;
                        if (DEBUG) {
                            System.out.println("Found non thrower");
                        }
                    }

                }
            } catch (ClassNotFoundException e) {
                analysisContext.getLookupFailureCallback().reportMissingClass(e);
            }
            boolean newResult = foundThrower && !foundNonThrower;
            if (newResult) {
                bugReporter.reportBug(new BugInstance(this, "TESTING", Priorities.NORMAL_PRIORITY)
                .addClassAndMethod(classContext.getJavaClass(), method)
                .addString("Call to method that always throws Exception").addMethod(primaryXMethod)
                .describe(MethodAnnotation.METHOD_CALLED).addSourceLine(classContext, method, loc));
            }

        }

    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(!testingEnabled){
            return;
        }
        analysisContext = AnalysisContext.currentAnalysisContext();
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {

                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError(
                        "Error checking for infinite recursive loop in "
                                + SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method), e);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError(
                        "Error checking for infinite recursive loop in "
                                + SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method), e);
            }
        }
    }

}
