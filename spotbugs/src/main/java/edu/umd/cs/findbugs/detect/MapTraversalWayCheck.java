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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;

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
public class MapTraversalWayCheck implements Detector {

    private static final String CLASS_MAP = "java/util/Map";

    private static final String CLASS_ITERATOR = "java/util/Iterator";

    private static final String CLASS_MAP_ENTRY = "java/util/Map\u0024Entry";

    private static final String CLASS_SET = "java/util/Set";

    private static final String METHOD_MAP_KEYSET = "keySet";

    private static final String METHOD_MAP_ENTRYSET = "entrySet";

    private static final String METHOD_ITERATOR = "iterator";

    private static final String METHOD_NEXT = "next";

    private static final int WAY_MAP_KEYSET = 0;

    private static final int WAY_MAP_ENTRYSET = 1;

    private static final int WAY_NOT_LOOP = -1;

    private final BugAccumulator bugAccumulator;

    private final BugReporter bugReporter;

    /**
     * @param bugReporter
     */
    public MapTraversalWayCheck(BugReporter bugReporter) {
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

            // Init method,skip
            String methodName = method.getName();
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                continue;
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
     * Analyze method
     *
     * @param classContext
     *            class context
     * @param method
     *            method to be analyzed
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    /**
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

        // constant pool of this method
        ConstantPoolGen constPool = classContext.getConstantPoolGen();

        // location list (instruction list)
        Collection<Location> locationCollection = cfg.orderedLocations();

        ArrayList<Location> locationList = new ArrayList<>();
        locationList.addAll(locationCollection);

        analyseMapTraversal(locationList, constPool, classContext, method);
    }

    /**
     * Analyse the way of traversling map
     *
     * @param locationList
     *            location list
     * @param constPool
     *            constant pool
     * @param classContext
     *            class context
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void analyseMapTraversal(ArrayList<Location> locationList, ConstantPoolGen constPool,
            ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {

        for (int i = 0; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle insHandle = location.getHandle();
            if (null == insHandle) {
                continue;
            }

            Instruction ins = insHandle.getInstruction();

            // bytecode: invokeinterface #{position} //InterfaceMethod java/util/Map.keySet()
            // or InterfaceMethod java/util/Map.entrySet()
            if (ins instanceof INVOKEINTERFACE) {
                int constIndex = ((INVOKEINTERFACE) ins).getIndex();

                // check is map traversal instruction
                int tmp = checkIsMapTravsal(constIndex, constPool);
                if (WAY_NOT_LOOP == tmp) {
                    continue;
                }

                // Find the cycle variable
                LocalVariable cycleVa = findCycleVariable(locationList, i, constPool, method, classContext);
                if (null == cycleVa) {
                    continue;
                }

                if (WAY_MAP_KEYSET == tmp) {
                    boolean res = checkMapKeysetValid(cycleVa, locationList, i, classContext, method, constPool);

                    if (!res) {
                        fillReport(tmp, location, classContext, method, constPool);
                    }
                } else if (WAY_MAP_ENTRYSET == tmp) {
                    boolean res = checkMapEntrysetValid(cycleVa, locationList, i, classContext, method, constPool);

                    if (!res) {
                        fillReport(tmp, location, classContext, method, constPool);
                    }
                }
            }
        }
    }

    /**
     * Find the cycle variable, for example: for(String key : nameMaps), "key" is the cycle variable
     *
     * @param locationList
     *            Location list
     * @param startIndex
     *            start index of list
     * @param constPool
     *            constant Pool
     * @param method
     *            method
     * @param classContext
     *            class context
     * @return the cycle variable
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private LocalVariable findCycleVariable(ArrayList<Location> locationList, int startIndex, ConstantPoolGen constPool,
            Method method, ClassContext classContext) throws DataflowAnalysisException, CFGBuilderException {
        LocalVariable cycleVa = null;
        String iteratorName = null;

        // when keySet() or entrySet() is called, next instruction must be INVOKEINTERFACE or StoreInstruction
        if (startIndex + 1 < locationList.size()) {
            Location location = locationList.get(startIndex + 1);
            InstructionHandle insHandleLoop = location.getHandle();

            Instruction ins = insHandleLoop.getInstruction();
            if (!(ins instanceof INVOKEINTERFACE) && !(ins instanceof StoreInstruction)) {
                return null;
            }

            if (ins instanceof INVOKEINTERFACE) {
                int constIndex = ((INVOKEINTERFACE) ins).getIndex();
                String className = getClassOrMethod(true, constIndex, constPool);
                String methodName = getClassOrMethod(false, constIndex, constPool);

                if (!CLASS_SET.equals(className) || !METHOD_ITERATOR.equals(methodName)) {
                    return null;
                }
            }
        } else {
            return null;
        }

        for (int i = startIndex + 1; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle insHandleLoop = location.getHandle();

            Instruction ins = insHandleLoop.getInstruction();
            if (!(ins instanceof INVOKEINTERFACE)) {
                continue;
            }

            int constIndex = ((INVOKEINTERFACE) ins).getIndex();
            String className = getClassOrMethod(true, constIndex, constPool);
            String methodName = getClassOrMethod(false, constIndex, constPool);

            /*
             * if set.iterator() is called, get the return value's name, for example: Iterator<String> it =
             * nameSet.iterator();, "it"is the return value's name
             */
            if (CLASS_SET.equals(className) && METHOD_ITERATOR.equals(methodName)) {
                InstructionHandle nextHandle = location.getHandle().getNext();
                if (null != nextHandle) {
                    Instruction insTmp = nextHandle.getInstruction();
                    if (insTmp instanceof StoreInstruction) {
                        int index = ((StoreInstruction) insTmp).getIndex();
                        LocalVariable localVa = method.getLocalVariableTable().getLocalVariable(index,
                                nextHandle.getNext().getPosition());
                        iteratorName = localVa == null ? "?" : localVa.getName();
                    }
                }
                continue;
            }

            if (CLASS_ITERATOR.equals(className) && METHOD_NEXT.equals(methodName)) {

                String objName = getFieldName(1, location, classContext, method);
                /*
                 * Check the value name which calls Iterator.next() is equal with the value's name returned by
                 * Set.iterator() If the iteratorName is null, which means the Set.iterator() is not saved to a
                 * variable, for example: for(String s: nameMap.keySet()), Set.iterator() is called default
                 */
                if (null != iteratorName && iteratorName.equals(objName)) {
                    /*
                     * When the Iterator.next() is called, get the local variable in loop
                     */
                    cycleVa = getLocalVariable(locationList, i + 1, locationList.size(), method);
                    break;
                }
            }
        }

        return cycleVa;
    }

    /**
     * When the traversing way of map is keyset, it is invalid to call "Map.get(key)"
     *
     * @param cycleVa
     *            cycle value
     * @param locationList
     *            location list
     * @param startPc
     *            start position of loop map
     * @param endPc
     *            end position of loop map
     * @param startIndex
     *            start index of location list
     * @param classContext
     *            class context
     * @param method
     *            method
     * @param constPool
     *            constant pool
     * @return result, true: valid, false: invalid
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private boolean checkMapKeysetValid(LocalVariable cycleVa, List<Location> locationList, int startIndex,
            ClassContext classContext, Method method, ConstantPoolGen constPool)
            throws DataflowAnalysisException, CFGBuilderException {
        boolean valid = true;
        Location startLocation = locationList.get(startIndex);
        String objName = getFieldName(1, startLocation, classContext, method);
        int startLoop = cycleVa.getStartPC();
        int endLoop = cycleVa.getLength() + startLoop;

        for (int i = startIndex; i < locationList.size(); i++) {
            Location loc = locationList.get(i);
            InstructionHandle insHandleLoop = loc.getHandle();
            int pos = insHandleLoop.getPosition();

            // check the instruction is in the map loop range
            if (pos < startLoop) {
                continue;
            } else if (pos >= endLoop) {
                break;
            }

            Instruction ins = insHandleLoop.getInstruction();
            if (!(ins instanceof INVOKEINTERFACE)) {
                continue;
            }

            int constIndex = ((INVOKEINTERFACE) ins).getIndex();
            String className = getClassOrMethod(true, constIndex, constPool);
            String methodName = getClassOrMethod(false, constIndex, constPool);

            /* If the way is keyset, the method-‘Map.get(key)’ is called, invalid */
            if (CLASS_MAP.equals(className) && "get".equals(methodName)) {
                String fieldName = getFieldName(0, loc, classContext, method);
                if (objName.equals(fieldName)) {
                    valid = false;
                    break;
                }
            }

        }

        return valid;
    }

    /**
     * When the traversing way of map is entryset, it is invalid to only call "Entry.getKey()" or "Entry.getValu()"
     *
     * @param cycleVa
     *            cycle value
     * @param locationList
     *            location list
     * @param startPc
     *            start position of loop map
     * @param endPc
     *            end position of loop map
     * @param startIndex
     *            start index of location list
     * @param classContext
     *            class context
     * @param method
     *            method
     * @param constPool
     *            constant pool
     * @return result, true: valid, false: invalid
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private boolean checkMapEntrysetValid(LocalVariable cycleVa, List<Location> locationList, int startIndex,
            ClassContext classContext, Method method, ConstantPoolGen constPool)
            throws DataflowAnalysisException, CFGBuilderException {
        boolean valid = true;
        int keyCount = 0;
        int valueCount = 0;
        int startLoop = cycleVa.getStartPC();
        int endLoop = cycleVa.getLength() + startLoop;

        for (int i = startIndex; i < locationList.size(); i++) {
            Location loc = locationList.get(i);
            InstructionHandle insHandleLoop = loc.getHandle();
            int pos = insHandleLoop.getPosition();

            if (pos < startLoop) {
                continue;
            } else if (pos >= endLoop) {
                break;
            }

            Instruction ins = insHandleLoop.getInstruction();
            if (!(ins instanceof INVOKEINTERFACE)) {
                continue;
            }

            int constIndex = ((INVOKEINTERFACE) ins).getIndex();
            String className = getClassOrMethod(true, constIndex, constPool);
            String methodName = getClassOrMethod(false, constIndex, constPool);

            /* If the way is entryset, the method-‘Entry.getKey() or Entry.getValue()’ is never called, invalid */
            if (CLASS_MAP_ENTRY.equals(className)) {
                if ("getKey".equals(methodName)) {
                    String valueName = getFieldName(1, loc, classContext, method);
                    if (cycleVa.getName().equals(valueName)) {
                        keyCount++;
                    }
                } else if ("getValue".equals(methodName)) {
                    String valueName = getFieldName(1, loc, classContext, method);
                    if (cycleVa.getName().equals(valueName)) {
                        valueCount++;
                    }
                }
            }

        }

        // if getKey or getValue is never called and the count of calling number is not equal, invalid
        if ((0 == keyCount || 0 == valueCount) && (keyCount != valueCount)) {
            valid = false;
        }

        return valid;
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
     * @param constPool
     *            constant pool
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void fillReport(int way, Location location, ClassContext classContext, Method method,
            ConstantPoolGen constPool) throws DataflowAnalysisException, CFGBuilderException {
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

        if (WAY_MAP_KEYSET == way) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "SPEC_MAP_KEYSET_INEFFICIENT_CHECK", NORMAL_PRIORITY)
                            .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                    sourceLineAnnotation);
        } else if (WAY_MAP_ENTRYSET == way) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "SPEC_MAP_ENTRYSET_INEFFICIENT_CHECK", NORMAL_PRIORITY)
                            .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                    sourceLineAnnotation);
        }
    }

    /**
     * Check the way of traversing Map
     *
     * @param constIndex
     *            the index in constant pool
     * @param constPool
     *            constant pool
     * @return way: -1: values or not map traversal; 0:keySet; 1: entrySet
     */
    private int checkIsMapTravsal(int constIndex, ConstantPoolGen constPool) {

        String className = getClassOrMethod(true, constIndex, constPool);
        String methodName = getClassOrMethod(false, constIndex, constPool);

        if (!CLASS_MAP.equals(className)) {
            return WAY_NOT_LOOP;
        }

        if (METHOD_MAP_KEYSET.equals(methodName)) {
            return WAY_MAP_KEYSET;
        } else if (METHOD_MAP_ENTRYSET.equals(methodName)) {
            return WAY_MAP_ENTRYSET;
        } else {
            return WAY_NOT_LOOP;
        }

    }

    /**
     * Get class name or method name
     *
     * @param flag
     *            true: get class name; false: get method name
     * @param constIndex
     *            index in constant pool
     * @param constPool
     *            constant pool
     * @return
     */
    private String getClassOrMethod(boolean flag, int constIndex, ConstantPoolGen constPool) {
        String res = null;
        ConstantCP constTmp = (ConstantCP) constPool.getConstant(constIndex);

        if (flag) {
            ConstantClass classInfo = (ConstantClass) constPool.getConstant(constTmp.getClassIndex());
            res = ((ConstantUtf8) constPool.getConstant(classInfo.getNameIndex())).getBytes();
        } else {
            ConstantNameAndType cnat = (ConstantNameAndType) constPool.getConstant(constTmp.getNameAndTypeIndex());
            res = ((ConstantUtf8) constPool.getConstant(cnat.getNameIndex())).getBytes();
        }
        return res;
    }

    /**
     * Get the local variable used in loop
     *
     * @param locationList
     *            location list
     * @param startIndex
     *            start index
     * @param endIndex
     *            end index
     * @param method
     *            method
     * @return the local variable used in loop
     */
    private LocalVariable getLocalVariable(List<Location> locationList, int startIndex, int endIndex, Method method) {
        int index = -1;
        LocalVariable localVa = null;
        for (int i = startIndex; i < endIndex; i++) {
            Location loc = locationList.get(i);
            Instruction insTmp = loc.getHandle().getInstruction();
            if (insTmp instanceof StoreInstruction) {

                // get the index of the entry in local variable table
                index = ((StoreInstruction) insTmp).getIndex();
                InstructionHandle nextHandle = loc.getHandle().getNext();
                localVa = method.getLocalVariableTable().getLocalVariable(index, nextHandle.getPosition());
                break;
            }

        }

        return localVa;
    }

    /**
     * Get the object name of operator of the instruction
     *
     * @param way
     *            whether has parameter,1: no parameter, 0: one parameter
     * @param location
     *            the location of instruction
     * @param classContext
     *            class context
     * @param method
     *            method
     * @return String object name
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private String getFieldName(int way, Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAtLocation(location);
        // If there is no parameter，the object is the top in stack
        ValueNumber valueNumber = vnaFrame.getTopValue();
        if (0 == way) {
            // If there is one parameter，the object is the second to last in stack
            valueNumber = vnaFrame.getValue(vnaFrame.getNumSlots() - 2);
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

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}
