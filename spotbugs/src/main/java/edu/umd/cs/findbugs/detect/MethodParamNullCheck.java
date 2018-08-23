/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Administrator
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * @since ?
 *
 */
public class MethodParamNullCheck implements Detector {

    private final BugAccumulator bugAccumulator;
    private final BugReporter bugReporter;

    public MethodParamNullCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            // Not a public method,skip
            if ((method.getAccessFlags() & Const.ACC_PUBLIC) == 0) {
                continue;
            }

            // Init method,skip
            String methodName = method.getName();
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                continue;
            }

            // No param,skip
            String signature = method.getSignature();
            int returnTypeStart = signature.indexOf(')');
            if (returnTypeStart < 0) {
                continue;
            }

            // No param exist in paramList,skip
            String paramList = signature.substring(0, returnTypeStart + 1);
            if ("".equals(paramList) || (paramList.indexOf('L') < 0 && paramList.indexOf('[') < 0)) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }

            bugAccumulator.reportAccumulatedBugs();
        }
    }

    /**
     * Description:Analyze current method
     *
     * @param classContext
     * @param method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        LineNumberTable lineNumberTable = method.getLineNumberTable();
        IsNullValueDataflow nullValueDataflow = classContext.getIsNullValueDataflow(method);
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        Map<Integer, LocalVariable> variablePcMap = getvariablePcMap(classContext, method);
        Collection<Location> locationCollection = cfg.orderedLocations();
        ArrayList<Location> locationList = new ArrayList<>();
        locationList.addAll(locationCollection);

        int locationListSize = locationList.size();

        for (int i = 1; i < locationListSize; i++) {
            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            if (neededAction(ins)) {
                int position = handle.getPosition();
                LocalVariable local = getVariableOfIns(position, variablePcMap);
                if (null == local) {
                    continue;
                }

                boolean varUnChecked = this.ifVariableChecked(locationList, localVariableTable, nullValueDataflow,
                        local, i);

                if (!varUnChecked) {
                    SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
                            .fromVisitedInstruction(classContext, methodGen, sourceFile, handle);

                    BugAnnotation variableAnnotation = new LocalVariableAnnotation(local.getName(), local.getIndex(),
                            position, lineNumberTable.getSourceLine(handle.getPosition()));
                    variableAnnotation.setDescription("LOCAL_VARIABLE_VALUE_OF");
                    bugAccumulator.accumulateBug(
                            new BugInstance(this, "SPEC_MEHTOD_PARAM_NULL_CHECK", HIGH_PRIORITY)
                                    .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                            sourceLineAnnotation);
                }

            }
        }

    }

    /**
     * Description:Check if the given localVariable has been nullchecked before been used to query
     *
     * @param locationList
     * @param localVariableTable
     * @param nullValueDataflow
     * @param local
     * @param cursor
     * @return
     * @throws DataflowAnalysisException
     */
    private boolean ifVariableChecked(ArrayList<Location> locationList, LocalVariableTable localVariableTable,
            Dataflow<IsNullValueFrame, IsNullValueAnalysis> nullValueDataflow, LocalVariable local, int cursor)
            throws DataflowAnalysisException {
        for (int j = cursor - 1; j >= 0; j--) {
            Location location = locationList.get(j);
            InstructionHandle handle = location.getHandle();
            Instruction loadIns = handle.getInstruction();
            if (loadIns instanceof ALOAD) {
                ALOAD load = (ALOAD) loadIns;
                int varIndex = load.getIndex();
                LocalVariable loadVar = localVariableTable.getLocalVariable(varIndex);
                IsNullValueFrame frame = nullValueDataflow.getFactAtLocation(location);
                if (local == loadVar) {
                    IsNullValue v = frame.getValue(varIndex);
                    if (v.isChecked()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Description:Get the reciever of the instruction,return null if not found
     *
     * @param position
     * @param variablePcMap
     * @return
     */
    private LocalVariable getVariableOfIns(int position, Map<Integer, LocalVariable> variablePcMap) {
        for (Entry<Integer, LocalVariable> entry : variablePcMap.entrySet()) {
            if (position == entry.getKey().intValue()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Description:Judge if ins is kind of query action
     *
     * @param ins
     * @return
     */
    private boolean neededAction(Instruction ins) {
        if (ins instanceof INVOKEVIRTUAL) {
            return true;
        }

        if (ins instanceof GETFIELD) {
            return true;
        }

        if (ins instanceof INVOKEINTERFACE) {
            return true;
        }

        if (ins instanceof INVOKESPECIAL) {
            return true;
        }
        return false;
    }

    /**
     * Description:Get the recievers of the checknull actions, and store them to a map
     *
     * @return
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private Map<Integer, LocalVariable> getvariablePcMap(ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);
        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        Map<Integer, LocalVariable> variablePcMap = new HashMap<>();
        Iterator<BasicBlock> bbIter = invDataflow.getCFG().blockIterator();
        // Find the receiver of InvokeInstruction or GetField
        while (bbIter.hasNext()) {
            BasicBlock basicBlock = bbIter.next();
            if (basicBlock.isNullCheck()) {
                InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
                Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();

                // Get the stack values at entry to the null check.
                IsNullValueFrame frame = invDataflow.getStartFact(basicBlock);
                if (!frame.isValid()) {
                    continue;
                }

                ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getStartFact(basicBlock);
                if (!vnaFrame.isValid()) {
                    continue;
                }

                ValueNumber valueNumber = vnaFrame.getInstance(exceptionThrower, classContext.getConstantPoolGen());
                Location location = new Location(exceptionThrowerHandle, basicBlock);
                int pc = location.getHandle().getPosition();
                int pcprev = location.getHandle().getPrev().getPosition();
                int index = -1;
                for (int i = 0; i < vnaFrame.getNumLocals(); i++) {
                    if (valueNumber.equals(vnaFrame.getValue(i))) {
                        index = i;
                    }
                }
                LocalVariable local = localVariableTable.getLocalVariable(index, pcprev);
                if (null == local) {
                    local = localVariableTable.getLocalVariable(index, pc);
                }
                if (isParam(index, local)) {
                    variablePcMap.put(Integer.valueOf(pc), local);
                }
            }
        }
        return variablePcMap;
    }

    /**
     * Description:Judge if the Variable is a parameter
     *
     * @param index
     * @param local
     * @return
     */
    private boolean isParam(int index, LocalVariable local) {
        if (-1 == index) {
            return false;
        }

        if (null == local) {
            return false;
        }

        if (local.getStartPC() != 0) {
            return false;
        }

        if (local.getName().equals("this")) {
            return false;
        }

        return true;
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

    public String getErrorInfoFromException(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "\r\n" + sw.toString() + "\r\n";
        } catch (Exception e2) {
            return "bad getErrorInfoFromException";
        }
    }

}
