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
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.StoreInstruction;

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

    private final BugReporter bugReporter;

    private final Map<String, Integer> globalIntFieldMap = new HashMap<>();

    private final int warningLevel = HIGH_PRIORITY;

    private ClassContext classCtx;

    /**
     * @param bugReporter
     */
    public ArrayIndexOutCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classCtx = classContext;
        Method[] methods = classContext.getJavaClass().getMethods();
        for (Method method : methods) {

            // Init method,skip
            if ("<init>".equals(method.getName()) || "<clinit>".equals(method.getName())) {
                try {
                    getGlobalIntFieldMap(method);
                    continue;
                } catch (CFGBuilderException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            try {
                analyzeMethod(method);
            } catch (Exception e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
        }
    }

    /**
     * Get global int field into map
     *
     * @param classCtx
     *            class context
     * @param initMethod
     *            init method
     * @throws CFGBuilderException
     */
    private void getGlobalIntFieldMap(Method initMethod) throws CFGBuilderException {
        CFG cfg = classCtx.getCFG(initMethod);

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
                    String fieldName = ((PUTFIELD) nextIns).getFieldName(classCtx.getConstantPoolGen());
                    Integer intNum = new Integer(((ICONST) ins).getValue().intValue());

                    globalIntFieldMap.put(fieldName, intNum);
                }

            }

        }
    }

    /**
     * Analyze method
     *
     * @param classCtx
     *            class context
     * @param method
     *            method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classCtx.getCFG(method);

        if (null == cfg) {
            return;
        }

        ValueNumberDataflow dataflow = classCtx.getValueNumberDataflow(method);

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
            // when encounter array.length or List.size()
            if (ins instanceof ARRAYLENGTH || ins instanceof INVOKEINTERFACE) {
                // get array length model, including arrayName, local lengthName, length's valueNumber
                ArrayLengthModel arrarLenModel = getArrayLengthExp(location, method);

                if (null != arrarLenModel) {
                    // get compare model, including arrayName, comapared number, compare expression, and so on
                    CheckCompareArrayAndNum(locationList, i, dataflow, arrarLenModel,
                            method);

                }

            }
        }
    }

    /**
     * Get array length model, For example, instruction is "int len = array.length", ArrayLengthModel.arrayName = array,
     * ArrayLengthModel.localLenName = len, ArrayLengthModel.valueNum is the top value number in stack
     *
     * @param location
     *            location
     * @param method
     *            method
     * @return ArrayLengthModel
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private ArrayLengthModel getArrayLengthExp(Location location, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberDataflow dataflow = classCtx.getValueNumberDataflow(method);
        InstructionHandle handle = location.getHandle();
        Instruction ins = handle.getInstruction();
        String lenName = null;
        ArrayLengthModel arrayLengthModel = new ArrayLengthModel();

        if (ins instanceof INVOKEINTERFACE) {
            String className = getClassOrMethodFromInstruction(true, ((INVOKEINTERFACE) ins).getIndex(),
                    classCtx.getConstantPoolGen());
            String methodName = getClassOrMethodFromInstruction(false, ((INVOKEINTERFACE) ins).getIndex(),
                    classCtx.getConstantPoolGen());

            if (!"java/util/List".equals(className) || !"size".equals(methodName)) {
                return null;
            }
        }

        String arrayName = getObjectName(true, dataflow, location, method);

        if (null == arrayName) {
            return null;
        }

        // get the stack after ARRAYLENGTH instruction
        ValueNumberFrame arrayLengthFrame = dataflow.getFactAfterLocation(location);
        ValueNumber nowValueNumber = arrayLengthFrame.getTopValue();
        arrayLengthModel.setArrayName(arrayName);
        arrayLengthModel.setValueNumber(nowValueNumber);

        // when array.length is stored in a local variable, for example: int len = array.length
        InstructionHandle nextHandle = handle.getNext();
        if (null == nextHandle) {
            return arrayLengthModel;
        }
        Instruction nextIns = nextHandle.getInstruction();

        if (nextIns instanceof StoreInstruction) {
            int locIndex = ((StoreInstruction) nextIns).getIndex();
            LocalVariable len = method.getLocalVariableTable().getLocalVariable(locIndex,
                    nextHandle.getNext().getPosition());
            if (null != len) {
                lenName = len.getName();
                arrayLengthModel.setLocalLenName(lenName);
            }
        }

        return arrayLengthModel;
    }

    /**
     * Find the if_icmpge instruction, and check it whether compare an array's length with an constant and access out of
     * bounds.
     *
     * @param locationList
     *            location list
     * @param startIndex
     *            compared instruction index in location list
     * @param dataflow
     *            data flow of method
     * @param arrayLengthModel
     *            length model
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void CheckCompareArrayAndNum(List<Location> locationList, int startIndex,
            ValueNumberDataflow dataflow, ArrayLengthModel arrayLengthModel, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ComparedArrayModel arrayModel = null;

        for (int i = startIndex; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();

            if (null == handle) {
                continue;
            }

            Instruction ins = handle.getInstruction();
            // find the compare instruction
            if (!(ins instanceof IfInstruction)) {
                continue;
            }

            int opCode = ins.getOpcode();
            switch (opCode) {
            // <
            case Const.IF_ICMPGE:
                // <=
            case Const.IF_ICMPGT:
                // >
            case Const.IF_ICMPLE:
                // >=
            case Const.IF_ICMPLT:
                // !=
            case Const.IF_ICMPEQ:
                // ==
            case Const.IF_ICMPNE:
                arrayModel = getCompareModelFromExp(location, arrayLengthModel, method);
                break;
            default:
                break;
            }

            if (null == arrayModel) {
                continue;
            }
            arrayModel.setCompareHandle(handle);
            Integer compareNum = getCompareNum(dataflow, locationList, i, arrayModel.getComapreNumValueNum());

            if (null != compareNum) {
                arrayModel.setCompareNum(compareNum.intValue());
                arrayModel.setCompareHandle(handle);
                checkAccessValid(i, arrayModel, locationList, method);
            }

        }

    }

    /**
     * Get compared information from compare expression, For example, compare expression is "if(array.length > 10)",
     * then ComparedArrayModel.name = array, ComparedArrayModel.compareNum=10, ComparedArrayModel.isLeftInCompare =
     * true, ComparedArrayModel.compareHandle = >,
     *
     * @param compareLocation
     *            compare location
     * @param arrayLengthModel
     *            array length model
     * @param method
     *            method
     * @return ComparedArrayModel
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private ComparedArrayModel getCompareModelFromExp(Location compareLocation, ArrayLengthModel arrayLengthModel,
            Method method) throws DataflowAnalysisException, CFGBuilderException {

        ValueNumberDataflow dataflow = classCtx.getValueNumberDataflow(method);
        ValueNumberFrame vnaFrame = dataflow.getFactAtLocation(compareLocation);
        // top value
        ValueNumber comparedRightValueNum = vnaFrame.getTopValue();
        // bottom value
        ValueNumber comparedLeftValueNum = vnaFrame.getValue(vnaFrame.getNumLocals());

        ComparedArrayModel compareModel = new ComparedArrayModel();
        String lenName = arrayLengthModel.getLocalLenName();
        ValueNumber arrayLenValueNum = arrayLengthModel.getValueNumber();

        compareModel.setName(arrayLengthModel.getArrayName());
        if (vnaFrame.getStackDepth() <= 0) {
            return null;
        }

        /*
         * if the array.length is in the top of compared stack, it means array.length is in the right of the compare
         * expression /* For example: if(5 > array.length)
         */
        if (arrayLenValueNum.equals(comparedRightValueNum)) {

            compareModel.setComapreNumValueNum(comparedLeftValueNum);
            compareModel.setLeftInCompare(false);
            // For example: if(array.length > 5)
        } else if (arrayLenValueNum.equals(comparedLeftValueNum)) {
            compareModel.setComapreNumValueNum(comparedRightValueNum);
            compareModel.setLeftInCompare(true);
        } else {
            if (null == lenName) {
                return null;
            }
            /*
             * when array.length stored in local variable-len, and len is changed in the process, /* the value number in
             * stack will be changed, so check the name is same with the local variable
             */
            LocalVariableAnnotation topLocal = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method,
                    compareLocation, comparedRightValueNum, vnaFrame);
            if (null != topLocal && lenName.equals(topLocal.getName())) {
                compareModel.setComapreNumValueNum(comparedLeftValueNum);
                compareModel.setLeftInCompare(false);
            } else {
                LocalVariableAnnotation bottomLenName = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method,
                        compareLocation, comparedLeftValueNum, vnaFrame);

                if (null != bottomLenName && lenName.equals(bottomLenName.getName())) {
                    compareModel.setComapreNumValueNum(comparedRightValueNum);
                    compareModel.setLeftInCompare(true);
                } else {
                    return null;
                }
            }
        }

        return compareModel;
    }

    /**
     * Check accessing the array or list is out of bounds
     *
     * @param startIndex
     *            stary index
     * @param compareModel
     *            compare model
     * @param locationList
     *            location list
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void checkAccessValid(int startIndex, ComparedArrayModel compareModel, List<Location> locationList,
            Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberDataflow dataflow = classCtx.getValueNumberDataflow(method);

        for (int i = startIndex; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();

            if (null == handle) {
                continue;
            }

            Instruction ins = handle.getInstruction();

            if (ins instanceof ArrayInstruction || ins instanceof INVOKEINTERFACE) {

                Integer accessIndex = null;

                if (ins instanceof INVOKEINTERFACE) {
                    String className = getClassOrMethodFromInstruction(true, ((INVOKEINTERFACE) ins).getIndex(),
                            classCtx.getConstantPoolGen());
                    String methodName = getClassOrMethodFromInstruction(false, ((INVOKEINTERFACE) ins).getIndex(),
                            classCtx.getConstantPoolGen());

                    if ("java/util/List".equals(className) && "get".equals(methodName)) {
                        // Get the index of accessing list
                        accessIndex = getAccessIndexArrayLoad(locationList, i, dataflow);
                    }
                } else {
                    // Get the index of accessing array
                    accessIndex = getAccessIndex(locationList, i, dataflow);
                }

                if (null == accessIndex) {
                    continue;
                }

                // get the accessed array's name
                String arrayName = getObjectName(false, dataflow, location, method);
                if (null != arrayName && arrayName.equals(compareModel.getName())) {

                    /*
                     * if the accessed array's length has been compared, and the accessing index is lager or equal than
                     * compared number
                     */
                    checkArrayOutBounds(location, compareModel, handle.getPosition(), accessIndex.intValue(), method);
                }
            }

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
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void checkArrayOutBounds(Location accessLocation, ComparedArrayModel arrayModel, int accessPc,
            int accessIndex, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        InstructionHandle handle = arrayModel.getCompareHandle();
        Instruction ins = handle.getInstruction();

        // if branch handle
        InstructionHandle falseHandle = ((IfInstruction) ins).getTarget();
        // else branch handle
        InstructionHandle trueHandle = handle.getNext();
        int opcode = ins.getOpcode();

        // array.length>=10 || 10<=array.length
        if (arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPLT
                || !arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPGT) {
            // if branch
            if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                if (accessIndex >= arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            } else {
                // else branch
                boolean hasReturn = checkHasReturn(falseHandle, accessPc);
                if (!hasReturn) {
                    fillWarningReport(accessLocation, method);
                } else if (accessIndex >= arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            }
        }

        // array.length>10 || 10<array.length
        if (arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPLE
                || !arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPGE) {
            // if branch
            if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                if (accessIndex > arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            } else {
                // else branch
                boolean hasReturn = checkHasReturn(falseHandle, accessPc);
                if (!hasReturn) {
                    fillWarningReport(accessLocation, method);
                } else if (accessIndex > arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            }
        }

        // array.length <= 10 || 10 >= array.length
        if (arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPGT
                || !arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPLT) {
            // if branch
            if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                fillWarningReport(accessLocation, method);
            } else {
                // else branch
                if (accessIndex > arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            }

        }

        // array.length < 10 || 10 > array.length
        if (arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPGE
                || !arrayModel.isLeftInCompare() && opcode == Const.IF_ICMPLE) {
            // if branch
            if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                fillWarningReport(accessLocation, method);
            } else {
                // else branch
                if (accessIndex >= arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            }

        }

        // array.length == 10 || 10 == array.length
        if (opcode == Const.IF_ICMPNE) {
            // if branch
            if (accessPc > trueHandle.getPosition() && accessPc < falseHandle.getPosition()) {
                if (accessIndex >= arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            } else {
                if (!checkHasReturn(falseHandle, accessPc)) {
                    fillWarningReport(accessLocation, method);
                } else {
                    if (accessIndex >= arrayModel.getCompareNum()) {
                        fillWarningReport(accessLocation, method);
                    }
                }
            }

        }
        // array.length != 10 || 10 != array.length
        if (opcode == Const.IF_ICMPEQ) {
            if (accessPc > falseHandle.getPosition()) {
                if (accessIndex >= arrayModel.getCompareNum()) {
                    fillWarningReport(accessLocation, method);
                }
            } else {
                fillWarningReport(accessLocation, method);
            }

        }

    }

    /**
     * Is there return or athrow instruction between start Handle and end position
     *
     * @param startHandle
     *            start instruction handle
     * @param endPc
     *            end position
     * @return true: has return or throw; false: no
     */
    private boolean checkHasReturn(InstructionHandle startHandle, int endPc) {
        boolean flag = false;
        InstructionHandle nowHandle = startHandle;
        int nowPc = nowHandle.getPosition();

        while (nowPc < endPc) {
            Instruction ins = nowHandle.getInstruction();
            if (ins instanceof ReturnInstruction || ins instanceof ATHROW) {
                flag = true;
                break;
            }

            nowHandle = nowHandle.getNext();
            if (null == nowHandle) {
                break;
            }
            nowPc = nowHandle.getPosition();
        }

        return flag;

    }

    /**
     * Get the number compared with array's length
     *
     * @param dataflow
     *            value number data flow
     * @param locationList
     *            location list
     * @param index
     *            index
     * @param compareValueNumber
     *            compared value number in stack
     * @param classCtx
     *            class context
     * @return the number compared with array's length
     * @throws DataflowAnalysisException
     */
    private Integer getCompareNum(ValueNumberDataflow dataflow, List<Location> locationList, int index,
            ValueNumber compareValueNumber) throws DataflowAnalysisException {
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
                String sig = ((GETFIELD) preIns).getSignature(classCtx.getConstantPoolGen());
                if ("I".equals(sig)) {
                    ValueNumberFrame arrayLengthFrame = dataflow.getFactAfterLocation(preLoc);
                    ValueNumber nowValueNumber = arrayLengthFrame.getTopValue();

                    if (nowValueNumber.equals(compareValueNumber)) {
                        String filedName = ((GETFIELD) preIns).getFieldName(classCtx.getConstantPoolGen());
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
     *            data flow of method
     * @param location
     *            location
     * @param method
     *            method
     * @return array name
     * @throws DataflowAnalysisException
     */
    private String getObjectName(boolean isTop, ValueNumberDataflow dataflow, Location location, Method method)
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
     * @param dataflow
     *            data flow of method
     * @param classCtx
     *            class context
     * @return index of accessing array
     * @throws DataflowAnalysisException
     */
    private Integer getAccessIndex(List<Location> locationList, int index, ValueNumberDataflow dataflow)
            throws DataflowAnalysisException {
        Integer accessIndex = null;

        int opCode = locationList.get(index).getHandle().getInstruction().getOpcode();
        switch (opCode) {
        case Const.IASTORE:
        case Const.LASTORE:
        case Const.FASTORE:
        case Const.DASTORE:
        case Const.AASTORE:
        case Const.BASTORE:
        case Const.CASTORE:
        case Const.SASTORE:
            accessIndex = getAccessIndexArrayStore(locationList, index, dataflow);
            break;

        case Const.IALOAD:
        case Const.LALOAD:
        case Const.FALOAD:
        case Const.DALOAD:
        case Const.AALOAD:
        case Const.BALOAD:
        case Const.CALOAD:
        case Const.SALOAD:
            accessIndex = getAccessIndexArrayLoad(locationList, index, dataflow);
            break;

        default:
            break;
        }

        return accessIndex;
    }


    /**
     * Get the access index of array store instruction, for example: array[1] = "test", 1 is the access index
     *
     * @param locationList
     *            location list
     * @param index
     *            index of array instruction in location list
     * @param dataflow
     *            data flow of method
     * @param classCtx
     *            class context
     * @return access index
     * @throws DataflowAnalysisException
     */
    private Integer getAccessIndexArrayStore(List<Location> locationList, int index, ValueNumberDataflow dataflow)
            throws DataflowAnalysisException {
        ValueNumberFrame vnaFrame = dataflow.getFactAtLocation(locationList.get(index));
        ValueNumber accessNum = null;
        Integer accessIndex = null;

        if (vnaFrame.getStackDepth() > 1) {
            // array[1] = "test", access index is always in the second of stack
            accessNum = vnaFrame.getStackValue(1);
        } else {
            return accessIndex;
        }

        for (int i = 1; i <= index; i++) {
            Location loc = locationList.get(index - i);
            InstructionHandle preHandle = loc.getHandle();
            Instruction preIns = preHandle.getInstruction();

            if (preIns instanceof ICONST) {
                ValueNumberFrame tmpFrame = dataflow.getFactAfterLocation(loc);
                ValueNumber tmpValueNum = tmpFrame.getTopValue();
                if (accessNum.equals(tmpValueNum)) {
                    accessIndex = new Integer(((ICONST) preIns).getValue().intValue());
                    break;
                }
            } else if (preIns instanceof GETFIELD) {
                ValueNumberFrame tmpFrame = dataflow.getFactAfterLocation(loc);
                ValueNumber tmpValueNum = tmpFrame.getTopValue();
                if (accessNum.equals(tmpValueNum)) {
                    String filedName = ((GETFIELD) preIns).getFieldName(classCtx.getConstantPoolGen());
                    accessIndex = globalIntFieldMap.get(filedName);
                    break;
                }

            }

        }

        return accessIndex;
    }

    /**
     * Get access index of array load instruction, for example: String str = array[1], 1 is the access index
     *
     * @param locationList
     *            location list
     * @param index
     *            index of array instruction in location list
     * @param dataflow
     *            data flow of method
     * @param classCtx
     *            class context
     * @return access index
     */
    private Integer getAccessIndexArrayLoad(List<Location> locationList, int index, ValueNumberDataflow dataflow) {
        Integer accessIndex = null;
        if (index - 1 < 0) {
            return accessIndex;
        }

        Location preLocation = locationList.get(index - 1);
        InstructionHandle preHandle = preLocation.getHandle();
        Instruction preIns = preHandle.getInstruction();

        if (preIns instanceof ICONST) {
            accessIndex = new Integer(((ICONST) preIns).getValue().intValue());

        } else if (preIns instanceof GETFIELD) {
            String filedName = ((GETFIELD) preIns).getFieldName(classCtx.getConstantPoolGen());
            accessIndex = globalIntFieldMap.get(filedName);
        }

        return accessIndex;
    }

    /**
     * Get class name or method name
     *
     * @param isClass
     *            true: get class name; false: get method name
     * @param constIndex
     *            index in constant pool
     * @param constPool
     *            constant pool
     * @return
     */
    private String getClassOrMethodFromInstruction(boolean isClass, int constIndex, ConstantPoolGen constPool) {
        String res = null;
        ConstantCP constTmp = (ConstantCP) constPool.getConstant(constIndex);

        if (isClass) {
            ConstantClass classInfo = (ConstantClass) constPool.getConstant(constTmp.getClassIndex());
            res = ((ConstantUtf8) constPool.getConstant(classInfo.getNameIndex())).getBytes();
        } else {
            ConstantNameAndType cnat = (ConstantNameAndType) constPool.getConstant(constTmp.getNameAndTypeIndex());
            res = ((ConstantUtf8) constPool.getConstant(cnat.getNameIndex())).getBytes();
        }
        return res;
    }

    /**
     * Fill the bug report
     *
     * @param location
     *            code location
     * @param classCtx
     *            class context
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void fillWarningReport(Location location, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == location) {
            return;
        }
        InstructionHandle insHandle = location.getHandle();
        MethodGen methodGen = classCtx.getMethodGen(method);
        String sourceFile = classCtx.getJavaClass().getSourceFileName();
        ValueNumberDataflow valueNumDataFlow = classCtx.getValueNumberDataflow(method);

        ValueNumberFrame vnaFrame = valueNumDataFlow.getFactAtLocation(location);

        ValueNumber valueNumber = vnaFrame.getValue(vnaFrame.getNumLocals());

        BugAnnotation variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame, "VALUE_OF");

        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classCtx, methodGen,
                sourceFile, insHandle);

        BugInstance bug = new BugInstance(this, "SPEC_ARRAY_INDEX_OUT_OF_BOUNDS", warningLevel);
        bug.addClassAndMethod(classCtx.getJavaClass(), method);
        bug.addOptionalAnnotation(variableAnnotation);
        bug.addSourceLine(sourceLineAnnotation);
        bugReporter.reportBug(bug);

    }

    @Override
    public void report() {

    }

    /**
     * Model which compared with array.length
     *
     * @since ?
     *
     */
    private static class ComparedArrayModel {

        /**
         * array name
         */
        private String name;

        /**
         * compared name
         */
        private int compareNum;

        /**
         * arrayLength is at left or right of compared expression
         */
        private boolean isLeftInCompare;

        /**
         * compared instruction handle
         */
        private InstructionHandle compareHandle;

        private ValueNumber comapreNumValueNum;

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

        public ValueNumber getComapreNumValueNum() {
            return comapreNumValueNum;
        }

        public void setComapreNumValueNum(ValueNumber comapreNumValueNum) {
            this.comapreNumValueNum = comapreNumValueNum;
        }

    }

    private static class ArrayLengthModel {
        private String arrayName;
        private ValueNumber valueNumber;
        private String localLenName;

        public ArrayLengthModel() {
        }

        public String getArrayName() {
            return arrayName;
        }

        public void setArrayName(String arrayName) {
            this.arrayName = arrayName;
        }

        public ValueNumber getValueNumber() {
            return valueNumber;
        }

        public void setValueNumber(ValueNumber valueNumber) {
            this.valueNumber = valueNumber;
        }

        public String getLocalLenName() {
            return localLenName;
        }

        public void setLocalLenName(String localLenName) {
            this.localLenName = localLenName;
        }
    }
}
