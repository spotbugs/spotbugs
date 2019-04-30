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

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
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
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;

/**
 * @since ?
 *
 */
public class CollectionIteratorCheck implements Detector {

    private final BugReporter bugReporter;
    private ClassContext classCtx;
    /**
     * @param bugReporter
     */
    public CollectionIteratorCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }


    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classCtx = classContext;
        Method[] methodList = this.classCtx.getJavaClass().getMethods();// 得到所有的方法列表
        for (Method method : methodList) {// 遍历方法
            if (method.getCode() == null) {
                continue;
            }
            if ("<init>".equals(method.getName()) || "<clinit>".equals(method.getName())) {// gouzao class init
                continue;
            }
            CFG cfg;
            try {
                cfg = classContext.getCFG(method);// 拿到方法
                if (null == cfg) {
                    return;
                }
                ConstantPoolGen constPool = classContext.getConstantPoolGen();
                Collection<Location> locationCollection = cfg.orderedLocations();// 得到方法的指令集
                ArrayList<Location> locationList = new ArrayList<>();
                locationList.addAll(locationCollection);
                analyzeContent(locationList, constPool, classContext, method);// 分析当前方法的所有指令集
            } catch (MethodUnprofitableException me) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", me);
            } catch (Exception e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
        }
    }

    private void analyzeContent(ArrayList<Location> locationList, ConstantPoolGen constPool, ClassContext classContext,
            Method method) throws DataflowAnalysisException, CFGBuilderException {
        for (int i = 0; i < locationList.size(); i++) {// 遍历指令
            Location location = locationList.get(i);// 得到具体指令
            InstructionHandle insHandle = location.getHandle();
            if (insHandle == null) {
                continue;
            }
            Instruction ins = insHandle.getInstruction();
            if (ins instanceof INVOKEINTERFACE || ins instanceof INVOKEVIRTUAL) {// 如果指令为invokeinterface进入
                int insIndex = 0;
                if (ins instanceof INVOKEINTERFACE) {
                    insIndex = ((INVOKEINTERFACE) ins).getIndex();
                } else if (ins instanceof INVOKEVIRTUAL) {
                    insIndex = ((INVOKEVIRTUAL) ins).getIndex();
                }
                String classNames = getClassOrMethodName(constPool, insIndex, true);
                String methodNames = getClassOrMethodName(constPool, insIndex, false);
                if (classNames.endsWith("Map")) {
                    if ("keySet".equals(methodNames) || "entrySet".equals(methodNames)) {
                        LocalVariable cycleVa = findCycleVariable(locationList, i, constPool, method, classContext);// map集合循环找循环变量

                        String iteratorMapName = null;
                        iteratorMapName = getFieldName(false, location, classContext, method);// 迭代的map集合
                        if (cycleVa != null && iteratorMapName != null) {
                            boolean flag = checkMapValid(cycleVa, iteratorMapName, locationList, i, classContext,
                                    method, constPool);
                            if (!flag) {
                                fillBugReport(location, method, "map");
                            }
                        }
                    }
                } else if ((classNames.endsWith("Set") || classNames.endsWith("List"))
                        && "iterator".equals(methodNames)) {
                    LocalVariable cycleVa = findCycleVariable(locationList, i, constPool, method, classContext);
                    String iteratorSetOrListName = null;
                    iteratorSetOrListName = getFieldName(false, location, classContext, method);
                    if (cycleVa != null && iteratorSetOrListName != null) {
                        boolean flag = checkListOrSetValid(cycleVa, iteratorSetOrListName, locationList, i,
                                classContext,
                                method, constPool);
                        if (!flag) {
                            if (getClassOrMethodName(constPool, insIndex, true).endsWith("Set")) {
                                fillBugReport(location, method, "set");

                            } else if (getClassOrMethodName(constPool, insIndex, true).endsWith("List")) {
                                fillBugReport(location, method, "list");
                            }

                        }
                    }

                }
            }
        }
    }

    private void fillBugReport(Location location, Method method, String type)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == location) {
            return;
        }

        InstructionHandle insHandle = location.getHandle();
        MethodGen methodGen = classCtx.getMethodGen(method);
        String sourceFile = classCtx.getJavaClass().getSourceFileName();
        ValueNumberDataflow valueNumDataFlow = classCtx.getValueNumberDataflow(method);

        ValueNumberFrame vnaFrame = valueNumDataFlow.getFactAtLocation(location);
        ValueNumber valueNumber = vnaFrame.getTopValue();

        BugAnnotation variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame, "VALUE_OF");

        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classCtx, methodGen,
                sourceFile, insHandle);
        BugInstance bug;
        if ("set".equals(type)) {
            bug = new BugInstance(this, "SPEC_SET_REMOVEORADD_CHECK", NORMAL_PRIORITY);
        } else if ("list".equals(type)) {
            bug = new BugInstance(this, "SPEC_LIST_REMOVEORADD_CHECK", NORMAL_PRIORITY);
        } else {
            bug = new BugInstance(this, "SPEC_MAP_REMOVEORPUT_CHECK", NORMAL_PRIORITY);
        }
        bug.addClassAndMethod(methodGen, sourceFile);
        bug.addOptionalAnnotation(variableAnnotation);
        bug.addSourceLine(sourceLineAnnotation);
        bugReporter.reportBug(bug);
    }

    private static boolean checkListOrSetValid(LocalVariable cycleVa, String iteratorSetOrListName,
            List<Location> locationList,
            int startIndex, ClassContext classContext, Method method, ConstantPoolGen constPool)
            throws DataflowAnalysisException, CFGBuilderException {
        boolean valid = true;
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
            if (!(ins instanceof INVOKEINTERFACE) && !(ins instanceof INVOKEVIRTUAL)) {
                continue;
            }
            int insIndex = 0;
            if (ins instanceof INVOKEINTERFACE) {
                insIndex = ((INVOKEINTERFACE) ins).getIndex();
            } else if (ins instanceof INVOKEVIRTUAL) {
                insIndex = ((INVOKEVIRTUAL) ins).getIndex();
            }
            String className = getClassOrMethodName(constPool, insIndex, true);
            String methodName = getClassOrMethodName(constPool, insIndex, false);
            if (className.endsWith("Set") && ("remove".equals(methodName) || "add".equals(methodName))) {
                String valueName = null;
                valueName = getFieldName(true, loc, classContext, method);

                if (null != valueName && iteratorSetOrListName.equals(valueName)) {
                    valid = false;
                }
            } else if (className.endsWith("List")
                    && ("remove".equals(methodName) || "add".equals(methodName))) {
                String valueName = null;
                valueName = getFieldName(true, loc, classContext, method);

                if (null != valueName && iteratorSetOrListName.equals(valueName)) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private static boolean checkMapValid(LocalVariable cycleVa, String iteratorSetOrListName,
            List<Location> locationList, int startIndex, ClassContext classContext, Method method,
            ConstantPoolGen constPool) throws DataflowAnalysisException, CFGBuilderException {
        boolean valid = true;
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
            if (!(ins instanceof INVOKEINTERFACE) && !(ins instanceof INVOKEVIRTUAL)) {
                continue;
            }
            int insIndex = 0;
            if (ins instanceof INVOKEINTERFACE) {
                insIndex = ((INVOKEINTERFACE) ins).getIndex();
            } else if (ins instanceof INVOKEVIRTUAL) {
                insIndex = ((INVOKEVIRTUAL) ins).getIndex();
            }
            String className = getClassOrMethodName(constPool, insIndex, true);
            String methodName = getClassOrMethodName(constPool, insIndex, false);

            if (className.endsWith("Map") && ("remove".equals(methodName) || "put".equals(methodName))) {
                String valueName = null;
                valueName = getFieldName(true, loc, classContext, method);

                if (null != valueName && iteratorSetOrListName.equals(valueName)) {
                    valid = false;
                }

            }
        }
        return valid;
    }

    private static String getFieldName(boolean flag, Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAtLocation(location);
        ValueNumber valueNumber = vnaFrame.getTopValue();
        if (flag) {
            valueNumber = vnaFrame.getValue(vnaFrame.getNumSlots() - 2);
        }
        FieldAnnotation filedAnn = ValueNumberSourceInfo.findFieldAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame);
        if (null != filedAnn) {
            return filedAnn.getFieldName();
        }
        LocalVariableAnnotation localAnn = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame);
        if (null != localAnn) {
            return localAnn.getName();
        }
        return null;
    }

    private static String getClassOrMethodName(ConstantPoolGen constPool, int constIndex, boolean isClass) {
        ConstantCP constTmp = (ConstantCP) constPool.getConstant(constIndex);
        ConstantClass classInfo = (ConstantClass) constPool.getConstant(constTmp.getClassIndex());
        String className = ((ConstantUtf8) constPool.getConstant(classInfo.getNameIndex())).getBytes();
        ConstantNameAndType cnat = (ConstantNameAndType) constPool.getConstant(constTmp.getNameAndTypeIndex());
        String methodName = ((ConstantUtf8) constPool.getConstant(cnat.getNameIndex())).getBytes();
        if (isClass) {
            return className;
        }
        return methodName;
    }

    private Map<String, String> findKeySetOrEntrySetNextValue(int startIndex, List<Location> locationList,
            ConstantPoolGen constPool, Method method) {
        String iteratorName = null;
        String setName = null;
        Map<String, String> result = new HashMap<>();
        if (startIndex + 1 >= locationList.size()) {
            return null;
        }
        Location secdlocation = locationList.get(startIndex + 1);
        InstructionHandle secdInsHandle = secdlocation.getHandle();
        Instruction secIns = secdInsHandle.getInstruction();
        if (secIns instanceof INVOKEINTERFACE || secIns instanceof INVOKEVIRTUAL) {
            int insIndex = 0;
            if (secIns instanceof INVOKEINTERFACE) {
                insIndex = ((INVOKEINTERFACE) secIns).getIndex();
            } else if (secIns instanceof INVOKEVIRTUAL) {
                insIndex = ((INVOKEVIRTUAL) secIns).getIndex();
            }
            String className = getClassOrMethodName(constPool, insIndex, true);
            String methodName = getClassOrMethodName(constPool, insIndex, false);
            if ("java/util/Set".equals(className) && "iterator".equals(methodName)) {
                InstructionHandle nextHandle = secdlocation.getHandle().getNext();
                if (null != nextHandle) {
                    Instruction insTmp = nextHandle.getInstruction();
                    if (insTmp instanceof StoreInstruction) {
                        int index = ((StoreInstruction) insTmp).getIndex();
                        LocalVariable localVa = null;
                        if (method.getLocalVariableTable() != null) {
                            localVa = method.getLocalVariableTable().getLocalVariable(index,
                                    nextHandle.getNext().getPosition());
                        }
                        iteratorName = localVa == null ? "?" : localVa.getName();
                        result.put("iterator", iteratorName);
                    }
                }
            } else {
                return null;
            }
        } else if (secIns instanceof StoreInstruction) {
            int index = ((StoreInstruction) secIns).getIndex();
            InstructionHandle nextHandle = secdlocation.getHandle().getNext();
            LocalVariable localVa = null;
            if (null != method.getLocalVariableTable() && null != nextHandle.getNext()) {
                localVa = method.getLocalVariableTable().getLocalVariable(index, nextHandle.getNext().getPosition());
            }
            setName = localVa == null ? "?" : localVa.getName();
            result.put("set", setName);
        } else {
            return null;
        }

        return result;
    }

    private LocalVariable findCycleVariable(ArrayList<Location> locationList, int startIndex, ConstantPoolGen constPool,
            Method method, ClassContext classContext) throws DataflowAnalysisException, CFGBuilderException {
        LocalVariable cycleVa = null;
        String iteratorName = null;
        String setName = null;
        Map<String, String> nextOptValue = findKeySetOrEntrySetNextValue(startIndex, locationList, constPool, method);
        if (null == nextOptValue) {
            return null;
        }
        iteratorName = nextOptValue.get("iterator");
        setName = nextOptValue.get("set");
        for (int i = startIndex + 2; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle insHandleLoop = location.getHandle();

            Instruction ins = insHandleLoop.getInstruction();
            if (!(ins instanceof INVOKEINTERFACE) && !(ins instanceof INVOKEVIRTUAL)) {
                continue;
            }
            int insIndex = 0;
            if (ins instanceof INVOKEINTERFACE) {
                insIndex = ((INVOKEINTERFACE) ins).getIndex();
            } else if (ins instanceof INVOKEVIRTUAL) {
                insIndex = ((INVOKEVIRTUAL) ins).getIndex();
            }
            String className = getClassOrMethodName(constPool, insIndex, true);
            String methodName = getClassOrMethodName(constPool, insIndex, false);
            if (null == iteratorName && className.endsWith("Set") && "iterator".equals(methodName)) {
                String objName = getFieldName(false, location, classContext, method);
                if (null == objName) {
                    continue;
                }
                InstructionHandle nextHandle = location.getHandle().getNext();
                if (null != nextHandle) {
                    Instruction insTmp = nextHandle.getInstruction();
                    if (insTmp instanceof StoreInstruction && objName.equals(setName)) {
                        int index = ((StoreInstruction) insTmp).getIndex();
                        LocalVariable localVa = null;
                        if (null != method.getLocalVariableTable()) {
                            localVa = method.getLocalVariableTable().getLocalVariable(index,
                                    nextHandle.getNext().getPosition());
                        }
                        iteratorName = localVa == null ? "?" : localVa.getName();
                    }
                }
                continue;
            }
            if ("java/util/Iterator".equals(className) && "next".equals(methodName)) {
                String objName = getFieldName(false, location, classContext, method);
                if ((null == iteratorName && "?".equals(objName))
                        || (null != iteratorName && iteratorName.equals(objName))) {
                    for (int s = i; s < locationList.size(); s++) {
                        InstructionHandle nextInst = locationList.get(s).getHandle().getNext();
                        if (nextInst != null) {
                            Instruction insTmp = nextInst.getInstruction();
                            if (insTmp instanceof StoreInstruction) {
                                int local = ((StoreInstruction) insTmp).getIndex();
                                LocalVariable localVa = null;
                                localVa = method.getLocalVariableTable().getLocalVariable(local,
                                        nextInst.getNext().getPosition());
                                if (localVa != null) {
                                    cycleVa = localVa;
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return cycleVa;
    }

    private LocalVariable getLocalVariable(List<Location> locationList, int startIndex, int endIndex, Method method) {
        int index = -1;
        LocalVariable localVa = null;
        for (int i = startIndex; i < endIndex; i++) {
            Location loc = locationList.get(i);
            Instruction insTmp = loc.getHandle().getInstruction();
            if (insTmp instanceof StoreInstruction) {
                index = ((StoreInstruction) insTmp).getIndex();
                InstructionHandle nextHandle = loc.getHandle().getNext();
                localVa = method.getLocalVariableTable().getLocalVariable(index, nextHandle.getPosition());
                break;
            }

        }

        return localVa;
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }
}
