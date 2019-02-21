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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;

/**
 * @since ?
 *
 */
public class ArrayIndexOutCheck implements Detector {

    private final BugAccumulator bugAccumulator;

    private final BugReporter bugReporter;

    private final Map<String, Integer> globalIntFieldMap = new HashMap<>();

    /**
     * @param bugReporter
     */
    public ArrayIndexOutCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methods = classContext.getJavaClass().getMethods();
        for (Method method : methods) {

            // Init method,skip
            if ("<init>".equals(method.getName()) || "<clinit>".equals(method.getName())) {
                try {
                    getGlobalIntFieldMap(classContext, method);
                    continue;
                } catch (CFGBuilderException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
        }

        bugAccumulator.reportAccumulatedBugs();
    }

    /**
     * Get global int field into map
     *
     * @param classContext
     *            class context
     * @param initMethod
     *            init method
     * @throws CFGBuilderException
     */
    private void getGlobalIntFieldMap(ClassContext classContext, Method initMethod) throws CFGBuilderException {
        CFG cfg = classContext.getCFG(initMethod);

        if (null == cfg) {
            return;
        }

        Collection<Location> locations = cfg.orderedLocations();
        List<Location> locationList = new ArrayList<>();
        locationList.addAll(locations);

        for (int i = 0; i < locationList.size(); i++) {
            Location loc = locationList.get(i);
            InstructionHandle handle = loc.getHandle();

            if (null == handle) {
                continue;
            }

            Instruction ins = handle.getInstruction();

            if (ins instanceof ICONST) {
                InstructionHandle nextHandle = handle.getNext();
                Instruction nextIns = nextHandle.getInstruction();
                if (nextIns instanceof PUTFIELD) {
                    String fieldName = ((PUTFIELD) nextIns).getFieldName(classContext.getConstantPoolGen());
                    Integer intNum = new Integer(((ICONST) ins).getValue().intValue());

                    globalIntFieldMap.put(fieldName, intNum);
                }

            }

        }
    }

    /**
     * Analyze method
     *
     * @param classContext
     *            class context
     * @param method
     *            method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);

        if (null == cfg) {
            return;
        }

        ValueNumberDataflow dataflow = classContext.getValueNumberDataflow(method);
        Map<String, ComparedArrayModel> compareMap = new HashMap<>();

        Collection<Location> locations = cfg.orderedLocations();
        List<Location> locationList = new ArrayList<>();
        locationList.addAll(locations);

        for (int i = 0; i < locationList.size(); i++) {

            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();

            if (null == handle) {
                continue;
            }

            Instruction ins = handle.getInstruction();

            if (ins instanceof IfInstruction) {
                /*
                 * Find the if_icmpge instruction, and check it whether compare an array's length with an constant. Then
                 * store the array and constant into map
                 */
                getCompareArrayAndNum(locationList, i, dataflow, method, compareMap, classContext);
            }

            if (ins instanceof ArrayInstruction) {
                // if there is no compare instruction, continue
                if (compareMap.isEmpty()) {
                    continue;
                }

                // Get the index of accessing array
                Integer accessIndex = getAccessIndex(locationList, i, classContext);

                if (null == accessIndex) {
                    continue;
                }

                // get the accessed array's name
                String arrayName = getVariableName(false, dataflow, location, method);
                if (null != arrayName) {
                    ComparedArrayModel arrayModel = compareMap.get(arrayName);

                    /*
                     * if the accessed array's length has been compared, and the accessing index is lager or equal than
                     * compared number
                     */
                    if (null != arrayModel) {
                        boolean res = checkArrayOutBounds(arrayModel, handle.getPosition(), accessIndex.intValue());
                        if (res) {
                            fillWarningReport(location, classContext, method);
                        }
                    }
                }
            }
        }

    }

    /**
     * Find the if_icmpge instruction, and check it whether compare an array's length with an constant. Then store the
     * array and constant into map
     *
     * @param locationList
     *            location list
     * @param index
     *            compared instruction index in location list
     * @param dataflow
     *            data flow
     * @param method
     *            method
     * @param compareMap
     *            compared information map
     * @param classContext
     *            class context
     * @throws DataflowAnalysisException
     */
    private void getCompareArrayAndNum(List<Location> locationList, int index, ValueNumberDataflow dataflow,
            Method method, Map<String, ComparedArrayModel> compareMap, ClassContext classContext)
            throws DataflowAnalysisException {

        ValueNumberFrame vnaFrame = dataflow.getFactAtLocation(locationList.get(index));

        if (vnaFrame.getStackDepth() <= 0) {
            return;
        }

        String arrayName = null;
        boolean isLengthLeft = false;

        // top value
        ValueNumber topValueNumber = vnaFrame.getTopValue();

        // bottom value
        ValueNumber bottomValueNumber = vnaFrame.getValue(vnaFrame.getNumLocals());

        ValueNumber otherValueNumber = null;

        for (int i = 1; i <= index; i++) {
            Location preLoc = locationList.get(index - i);
            InstructionHandle preHandle = preLoc.getHandle();
            Instruction preIns = preHandle.getInstruction();

            if (preIns instanceof ARRAYLENGTH) {
                ValueNumberFrame arrayLengthFrame = dataflow.getFactAfterLocation(preLoc);
                ValueNumber nowValueNumber = arrayLengthFrame.getTopValue();

                // if the array.length is in the top of compared stack, it means array.length is in the right of the
                // compare expression
                // For example: if(5 > array.length)
                if (nowValueNumber.equals(topValueNumber)) {
                    arrayName = getVariableName(true, dataflow, preLoc, method);

                    if (null == arrayName) {
                        return;
                    }

                    otherValueNumber = bottomValueNumber;
                    isLengthLeft = false;

                    // For example: if(array.length > 5)
                } else if (nowValueNumber.equals(bottomValueNumber)) {
                    arrayName = getVariableName(true, dataflow, preLoc, method);

                    if (null == arrayName) {
                        return;
                    }

                    otherValueNumber = topValueNumber;
                    isLengthLeft = true;
                }
            }

        }

        Integer compareNum = getCompareNum(dataflow, locationList, index, otherValueNumber, classContext);

        if (null != compareNum) {
            ComparedArrayModel arrayModel = new ComparedArrayModel();
            arrayModel.setCompareNum(compareNum.intValue());
            arrayModel.setName(arrayName);
            arrayModel.setLeftInCompare(isLengthLeft);
            arrayModel.setCompareHandle(locationList.get(index).getHandle());
            compareMap.put(arrayName, arrayModel);
        }
    }

    /**
     * Check accessing array is out of bounds
     *
     * @param arrayModel
     *            compared array model
     * @param accessPc
     *            access instruction position
     * @param accessIndex
     *            access index
     * @return true: out of bounds
     */
    private boolean checkArrayOutBounds(ComparedArrayModel arrayModel, int accessPc, int accessIndex) {
        InstructionHandle handle = arrayModel.getCompareHandle();
        Instruction ins = handle.getInstruction();

        if (ins instanceof IfInstruction) {
            // if branch handle
            InstructionHandle falseHandle = ((IfInstruction) ins).getTarget();
            // else branch handle
            InstructionHandle trueHandle = handle.getNext();
            int opcode = ins.getOpcode();

            switch (opcode) {
            case Const.IF_ICMPGE:
                if (arrayModel.isLeftInCompare()) {
                    // accessing instruction is in if branch
                    if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum() - 1) {
                            return true;
                        }
                    }
                } else {
                    // accessing instruction is in else branch
                    if (accessPc > falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum()) {
                            return true;
                        }
                    }
                }
                break;
            case Const.IF_ICMPGT:
                if (arrayModel.isLeftInCompare()) {

                    if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum()) {
                            return true;
                        }
                    }
                } else {

                    if (accessPc > falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum() - 1) {
                            return true;
                        }
                    }
                }
                break;
            case Const.IF_ICMPLT:
                if (arrayModel.isLeftInCompare()) {
                    if (accessPc > falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum() - 1) {
                            return true;
                        }
                    }
                } else {
                    if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum()) {
                            return true;
                        }
                    }
                }
                break;
            case Const.IF_ICMPLE:
                if (arrayModel.isLeftInCompare()) {
                    if (accessPc > falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum()) {
                            return true;
                        }
                    }
                } else {
                    if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                        if (accessIndex >= arrayModel.getCompareNum() - 1) {
                            return true;
                        }
                    }
                }
                break;
            default:
                break;
            }
        }

        return false;
    }

    /**
     * Get the number compared with array's length
     *
     * @param locationList
     *            location list
     * @param index
     *            index
     * @param classContext
     *            class context
     * @return the number compared with array's length
     * @throws DataflowAnalysisException
     */
    private Integer getCompareNum(ValueNumberDataflow dataflow, List<Location> locationList, int index,
            ValueNumber compareValueNumber, ClassContext classContext) throws DataflowAnalysisException {
        Integer compareNum = null;

        for (int i = 1; i <= index; i++) {
            Location preLoc = locationList.get(index - i);
            InstructionHandle preHandle = preLoc.getHandle();
            Instruction preIns = preHandle.getInstruction();

            // compared with an int constant, for example: if(array.length > 10)
            if (preIns instanceof ICONST) {
                ValueNumberFrame arrayLengthFrame = dataflow.getFactAfterLocation(preLoc);
                ValueNumber nowValueNumber = arrayLengthFrame.getTopValue();

                if (nowValueNumber.equals(compareValueNumber)) {
                    compareNum = new Integer(((ICONST) preIns).getValue().intValue());
                    break;
                }
            }

            // compared with an global constant field, for example: if(array.length > MAX_LENGTH)
            if (preIns instanceof GETFIELD) {
                String sig = ((GETFIELD) preIns).getSignature(classContext.getConstantPoolGen());
                if ("I".equals(sig)) {
                    ValueNumberFrame arrayLengthFrame = dataflow.getFactAfterLocation(preLoc);
                    ValueNumber nowValueNumber = arrayLengthFrame.getTopValue();

                    if (nowValueNumber.equals(compareValueNumber)) {
                        String filedName = ((GETFIELD) preIns).getFieldName(classContext.getConstantPoolGen());
                        compareNum = globalIntFieldMap.get(filedName);
                        break;
                    }
                } else {
                    continue;
                }
            }
        }

        return compareNum;
    }

    /**
     * Get variable name
     *
     * @param isTop
     *            whether array is in top of statk
     * @param dataflow
     *            data flow
     * @param location
     *            location
     * @param method
     *            method
     * @return array name
     * @throws DataflowAnalysisException
     */
    private String getVariableName(boolean isTop, ValueNumberDataflow dataflow, Location location, Method method)
            throws DataflowAnalysisException {
        ValueNumberFrame vnaFrame = dataflow.getFactAtLocation(location);
        if (vnaFrame.getStackDepth() < 0) {
            return null;
        }

        ValueNumber valueNumber = null;

        if (isTop) {
            valueNumber = vnaFrame.getTopValue();
        } else {
            // bottom value
            valueNumber = vnaFrame.getValue(vnaFrame.getNumLocals());
        }

        // golable variable
        FieldAnnotation filedAnn = ValueNumberSourceInfo.findFieldAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame);

        if (null != filedAnn) {
            return filedAnn.getFieldName();
        }

        // local variable
        LocalVariableAnnotation localAnn = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame);
        if (null != localAnn) {
            return localAnn.getName();
        }

        return null;
    }

    /**
     * Get index of accessing array
     *
     * @param locationList
     *            location list
     * @param index
     *            index
     * @param classContext
     *            class context
     * @return index of accessing array
     */
    private Integer getAccessIndex(List<Location> locationList, int index, ClassContext classContext) {
        Integer accessIndex = null;

        for (int i = 1; i <= 2; i++) {
            if (index - i < 0) {
                return accessIndex;
            }

            Location preLocation = locationList.get(index - i);
            InstructionHandle preHandle = preLocation.getHandle();
            Instruction preIns = preHandle.getInstruction();

            if (preIns instanceof ICONST) {
                accessIndex = new Integer(((ICONST) preIns).getValue().intValue());

            } else if (preIns instanceof GETFIELD) {
                String filedName = ((GETFIELD) preIns).getFieldName(classContext.getConstantPoolGen());
                accessIndex = globalIntFieldMap.get(filedName);
            }

        }

        return accessIndex;
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
    private void fillWarningReport(Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == location) {
            return;
        }

        InstructionHandle insHandle = location.getHandle();
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();
        ValueNumberDataflow valueNumDataFlow = classContext.getValueNumberDataflow(method);

        ValueNumberFrame vnaFrame = valueNumDataFlow.getFactAtLocation(location);
        ValueNumber valueNumber = vnaFrame.getTopValue();

        BugAnnotation variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame, "VALUE_OF");

        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                sourceFile, insHandle);

        bugAccumulator.accumulateBug(
                new BugInstance(this, "SPEC_ARRAY_INDEX_OUT_OF_BOUNDS", HIGH_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                sourceLineAnnotation);
    }

    @Override
    public void report() {

    }

    private static class ComparedArrayModel {
        private String name;

        private int compareNum;

        private boolean isLeftInCompare;

        private InstructionHandle compareHandle;

        /**
         *
         */
        public ComparedArrayModel() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCompareNum() {
            return compareNum;
        }

        public void setCompareNum(int compareNum) {
            this.compareNum = compareNum;
        }

        public boolean isLeftInCompare() {
            return isLeftInCompare;
        }

        public void setLeftInCompare(boolean isLeftInCompare) {
            this.isLeftInCompare = isLeftInCompare;
        }

        public InstructionHandle getCompareHandle() {
            return compareHandle;
        }

        public void setCompareHandle(InstructionHandle compareHandle) {
            this.compareHandle = compareHandle;
        }

    }

}
