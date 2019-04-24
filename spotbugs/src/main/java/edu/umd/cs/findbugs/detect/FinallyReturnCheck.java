/*
 * Contributions to SpotBugs
 * Copyright (C) 2019, Administrator
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;

/**
 * @since ?
 *
 */
public class FinallyReturnCheck implements Detector {

    private final BugReporter bugReporter;

    /**
     * @param bugReporter
     */
    public FinallyReturnCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            // Init method,skip
            String methodName = method.getName();
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (Exception e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
        }

    }

    /**
     * Analyse the method
     *
     * @param classContext
     * @param method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        if (null == cfg) {
            return;
        }

        // get the exception table
        CodeExceptionGen[] exceptions = cfg.getMethodGen().getExceptionHandlers();

        List<CodeExceptionGen> finallyList = new ArrayList<>();
        // get the finally block
        for (CodeExceptionGen exception : exceptions) {
            InstructionHandle handleTmp = exception.getEndPC();
            Instruction ins = null;

            if (null != handleTmp) {
                ins = handleTmp.getInstruction();
            } else {
                continue;
            }

            // when catchType is null, and not syncronized, it means finally start
            if (null == exception.getCatchType() && !(ins instanceof MONITOREXIT)) {
                finallyList.add(exception);
            }
        }

        // if there is no finally, return directory
        if (finallyList.size() <= 0) {
            return;
        }

        // the instruction list
        Collection<Location> locationCollection = cfg.orderedLocations();

        ArrayList<Location> locationList = new ArrayList<>();
        locationList.addAll(locationCollection);

        for (CodeExceptionGen exception : finallyList) {
            InstructionHandle targetHandle = exception.getHandlerPC();
            int targetPc = targetHandle.getPosition();

            // check the finally block has return
            for (Location location : locationList) {
                InstructionHandle insHandle = location.getHandle();

                if (null == insHandle) {
                    continue;
                }

                int pc = insHandle.getPosition();
                // analyze from the finally start pc
                if (pc < targetPc) {
                    continue;
                }

                Instruction ins = insHandle.getInstruction();
                // if encounter GotoInstruction, check the instruction from the target instruction
                if (ins instanceof GotoInstruction) {
                    InstructionHandle gotoTargetHandle = ((GotoInstruction) ins).getTarget();
                    int gotoTarget = gotoTargetHandle.getPosition();
                    if (gotoTarget > targetPc) {
                        targetPc = gotoTargetHandle.getPosition();
                    }
                    continue;
                }

                // when encounter ReturnInstruction in finally block, fill the report ang break.
                if (ins instanceof ReturnInstruction) {
                    fillReport(location, classContext, method);
                    break;
                }
                // when encounter athrow instruction, it means the finally block is end.
                if (ins instanceof ATHROW) {
                    break;
                }

            }
        }

    }

    /**
     * Fill the bug report
     *
     * @param location
     *            code location
     * @param classContext
     *            class context
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void fillReport(Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == location) {
            return;
        }

        InstructionHandle insHandle = location.getHandle();
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                sourceFile, insHandle);
        BugInstance bug = new BugInstance(this, "SPEC_FINALLY_RETURN_CHECK", HIGH_PRIORITY);
        bug.addClassAndMethod(methodGen, sourceFile);
        bug.addSourceLine(sourceLineAnnotation);
        bugReporter.reportBug(bug);
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}
