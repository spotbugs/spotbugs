/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;

/**
 * Find potential SQL injection vulnerabilities.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 * @author Matt Hargett
 */
public class FindSqlInjection implements Detector {
    private static class StringAppendState {
        // remember the smallest position at which we saw something that concerns us
        int sawOpenQuote = Integer.MAX_VALUE;
        int sawCloseQuote = Integer.MAX_VALUE;
        int sawComma = Integer.MAX_VALUE;
        int sawAppend = Integer.MAX_VALUE;
        int sawUnsafeAppend = Integer.MAX_VALUE;

        public boolean getSawOpenQuote(InstructionHandle handle) {
            return sawOpenQuote <= handle.getPosition();
        }

        public boolean getSawCloseQuote(InstructionHandle handle) {
            return sawCloseQuote <= handle.getPosition();
        }

        public boolean getSawComma(InstructionHandle handle) {
            return sawComma <= handle.getPosition();
        }

        public boolean getSawAppend(InstructionHandle handle) {
            return sawAppend <= handle.getPosition();
        }

        public boolean getSawUnsafeAppend(InstructionHandle handle) {
            return sawUnsafeAppend <= handle.getPosition();
        }

        public void setSawOpenQuote(InstructionHandle handle) {
            sawOpenQuote = Math.min(sawOpenQuote, handle.getPosition());
        }

        public void setSawCloseQuote(InstructionHandle handle) {
            sawCloseQuote = Math.min(sawCloseQuote, handle.getPosition());
        }

        public void setSawComma(InstructionHandle handle) {
            sawComma = Math.min(sawComma, handle.getPosition());
        }

        public void setSawAppend(InstructionHandle handle) {
            sawAppend = Math.min(sawAppend, handle.getPosition());
        }

        public void setSawUnsafeAppend(InstructionHandle handle) {
            sawUnsafeAppend = Math.min(sawUnsafeAppend, handle.getPosition());
        }

    }

    BugReporter bugReporter;

    public FindSqlInjection(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null)
                continue;

            try {
                analyzeMethod(classContext, method);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("FindSqlInjection caught exception while analyzing " + methodGen, e);
            } catch (CFGBuilderException e) {
                bugReporter.logError("FindSqlInjection caught exception while analyzing " + methodGen, e);
            } catch (RuntimeException e) {
                System.out.println("Exception while checking for SQL injection in " + methodGen + " in "
                        + javaClass.getSourceFileName());
                e.printStackTrace(System.out);
            }
        }
    }

    private boolean isStringAppend(Instruction ins, ConstantPoolGen cpg) {
        if (ins instanceof INVOKEVIRTUAL) {
            INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) ins;

            if (invoke.getMethodName(cpg).equals("append") && invoke.getClassName(cpg).startsWith("java.lang.StringB")) {
                String sig = invoke.getSignature(cpg);
                char firstChar = sig.charAt(1);
                return firstChar == '[' || firstChar == 'L';
            }
        }

        return false;
    }

    private boolean isConstantStringLoad(Instruction ins, ConstantPoolGen cpg) {
        if (ins instanceof LDC) {
            LDC load = (LDC) ins;
            Object value = load.getValue(cpg);
            if (value instanceof String) {
                return true;
            }
        }

        return false;
    }

    private StringAppendState updateStringAppendState(InstructionHandle handle, ConstantPoolGen cpg,
            StringAppendState stringAppendState) {
        Instruction ins = handle.getInstruction();
        if (!isConstantStringLoad(ins, cpg)) {
            throw new IllegalArgumentException("instruction must be LDC");
        }

        LDC load = (LDC) ins;
        Object value = load.getValue(cpg);
        String stringValue = ((String) value).trim();
        if (stringValue.startsWith(",") || stringValue.endsWith(","))
            stringAppendState.setSawComma(handle);
        if (stringValue.endsWith("'"))
            stringAppendState.setSawOpenQuote(handle);
        if (stringValue.startsWith("'"))
            stringAppendState.setSawCloseQuote(handle);

        return stringAppendState;
    }

    private boolean isPreparedStatementDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
        if (!(ins instanceof INVOKEINTERFACE)) {
            return false;
        }

        INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

        String methodName = invoke.getMethodName(cpg);
        String interfaceName = invoke.getClassName(cpg);
        if (methodName.equals("prepareStatement") && interfaceName.equals("java.sql.Connection")) {
            return true;
        }

        return false;
    }

    private boolean isExecuteDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
        if (!(ins instanceof INVOKEINTERFACE)) {
            return false;
        }

        INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

        String methodName = invoke.getMethodName(cpg);
        String interfaceName = invoke.getClassName(cpg);
        if (methodName.startsWith("execute") && interfaceName.equals("java.sql.Statement")) {
            return true;
        }

        return false;
    }

    private boolean isDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
        return isPreparedStatementDatabaseSink(ins, cpg) || isExecuteDatabaseSink(ins, cpg);
    }

    private StringAppendState getStringAppendState(CFG cfg, ConstantPoolGen cpg) {
        StringAppendState stringAppendState = new StringAppendState();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();
            if (isConstantStringLoad(ins, cpg)) {
                stringAppendState = updateStringAppendState(handle, cpg, stringAppendState);
            } else if (isStringAppend(ins, cpg)) {
                stringAppendState.setSawAppend(handle);

                InstructionHandle prev = getPreviousInstruction(cfg, location, true);
                if (prev != null) {
                    Instruction prevIns = prev.getInstruction();
                    if (!isSafeValue(prevIns, cpg))
                        stringAppendState.setSawUnsafeAppend(handle);
                } else {
                    // FIXME: when would prev legitimately be null, and why
                    // would we report?
                    AnalysisContext.logError("In FindSqlInjection, saw null previous in "
                            + cfg.getMethodGen().getClassName() + "." + cfg.getMethodName());
                    stringAppendState.setSawUnsafeAppend(handle);
                }
            }
        }

        return stringAppendState;
    }

    private boolean isSafeValue(Instruction prevIns, ConstantPoolGen cpg) {
        if (prevIns instanceof LDC || prevIns instanceof GETSTATIC)
            return true;
        if (prevIns instanceof InvokeInstruction) {
            String methodName = ((InvokeInstruction) prevIns).getMethodName(cpg);
            if (methodName.startsWith("to") && methodName.endsWith("String") && methodName.length() > 8)
                return true;
        }
        return false;
    }

    private @CheckForNull
    InstructionHandle getPreviousInstruction(InstructionHandle handle, boolean skipNops) {
        while (handle.getPrev() != null) {
            handle = handle.getPrev();
            Instruction prevIns = handle.getInstruction();
            if (!skipNops && !(prevIns instanceof NOP)) {
                return handle;
            }
        }
        return null;
    }

    private @CheckForNull
    InstructionHandle getPreviousInstruction(CFG cfg, Location startLocation, boolean skipNops) {
        Location loc = startLocation;
        InstructionHandle prev = getPreviousInstruction(loc.getHandle(), skipNops);
        if (prev != null)
            return prev;
        BasicBlock block = loc.getBasicBlock();
        while (true) {
            block = cfg.getPredecessorWithEdgeType(block, EdgeTypes.FALL_THROUGH_EDGE);
            if (block == null)
                return null;
            InstructionHandle lastInstruction = block.getLastInstruction();
            if (lastInstruction != null)
                return lastInstruction;
        }
    }

    private BugInstance generateBugInstance(JavaClass javaClass, MethodGen methodGen, InstructionHandle handle,
            StringAppendState stringAppendState) {
        Instruction instruction = handle.getInstruction();
        ConstantPoolGen cpg = methodGen.getConstantPool();
        int priority = LOW_PRIORITY;
        if (stringAppendState.getSawAppend(handle)) {
            if (stringAppendState.getSawOpenQuote(handle) && stringAppendState.getSawCloseQuote(handle)) {
                priority = HIGH_PRIORITY;
            } else if (stringAppendState.getSawComma(handle)) {
                priority = NORMAL_PRIORITY;
            }

            if (!stringAppendState.getSawUnsafeAppend(handle)) {
                priority += 2;
            }
        }

        String description = "";
        if (isExecuteDatabaseSink(instruction, cpg)) {
            description = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE";
        } else if (isPreparedStatementDatabaseSink(instruction, cpg)) {
            description = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING";
        }

        BugInstance bug = new BugInstance(this, description, priority);
        bug.addClassAndMethod(methodGen, javaClass.getSourceFileName());

        return bug;
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws DataflowAnalysisException,
            CFGBuilderException {
        JavaClass javaClass = classContext.getJavaClass();
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null)
            return;

        ConstantPoolGen cpg = methodGen.getConstantPool();
        CFG cfg = classContext.getCFG(method);

        StringAppendState stringAppendState = getStringAppendState(cfg, cpg);

        ConstantDataflow dataflow = classContext.getConstantDataflow(method);
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            Instruction ins = location.getHandle().getInstruction();
            if (isDatabaseSink(ins, cpg)) {
                ConstantFrame frame = dataflow.getFactAtLocation(location);
                Constant value = frame.getStackValue(0);

                if (!value.isConstantString()) {
                    // TODO: verify it's the same string represented by
                    // stringAppendState
                    // FIXME: will false positive on const/static strings
                    // returns by methods
                    InstructionHandle prev = getPreviousInstruction(cfg, location, true);
                    if (prev == null || !isSafeValue(prev.getInstruction(), cpg)) {
                        BugInstance bug = generateBugInstance(javaClass, methodGen, location.getHandle(),
                                stringAppendState);
                        bug.addSourceLine(classContext, methodGen, javaClass.getSourceFileName(), location.getHandle());
                        bugReporter.reportBug(bug);
                    }
                }
            }
        }
    }

    public void report() {
    }
}

// vim:ts=4
