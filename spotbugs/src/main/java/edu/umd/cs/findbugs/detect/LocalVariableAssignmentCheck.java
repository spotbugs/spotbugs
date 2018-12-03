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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.StoreInstruction;

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
 * Analyse whether String or eight basic types of objects are assigned the same constants in all branches.
 */
public class LocalVariableAssignmentCheck implements Detector {
    private final HashSet<String> analyseTypes = new HashSet<>(
            Arrays.asList("java.lang.String", "boolean", "byte", "char", "short", "int", "long", "float", "double"));

    private final BugReporter bugReporter;

    /**
     * @param bugReporter
     *            as you see
     */
    public LocalVariableAssignmentCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            String methodName = method.getName();

            /*
             * Constructors and hashCode methods have more IDE automatically generated variables, so skip these methods.
             */
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName) || "hashCode".equals(methodName)) {
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
     * @param classContext
     * @param method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException {
        HashSet<Integer> gotoTargets = new HashSet<>();
        LocalVariableTable localVariableTable = method.getLocalVariableTable();

        if ((null == localVariableTable) || (localVariableTable.getTableLength() == 0)) {
            return;
        }

        HashMap<LocalVariable, LocalVariableAnalysis> analysisMap = initAnalysisMap(localVariableTable);
        if (analysisMap.size() == 0) {
            return;
        }

        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        // Sort the location according to their position at the class file
        ArrayList<Location> locationList = getSortedLocationIterator(cfg.locationIterator());
        for (int i = 1; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            /*
             * Record the location of the Goto statement's target instruction. If the subsequent write instruction is
             * Goto's target, the write instruction may have multiple assignment sources, so the analysis result needs
             * to be set to false.
             */
            if (ins instanceof GotoInstruction) {
                gotoTargets.add(((GotoInstruction) ins).getTarget().getPosition());
            }

            /* Analysis of all instructions written to variables */
            if (neededAnaysis(ins)) {
                int index = ((LocalVariableInstruction) ins).getIndex();
                LocalVariable localVariable = localVariableTable.getLocalVariable(index, handle.getPosition());
                if (null == localVariable) {
                    /*
                     * The pc of the first write operation of a variable, is less than the valid startPc of the variable
                     */
                    localVariable = localVariableTable.getLocalVariable(index, handle.getPosition() + 2);
                }
                if (null == localVariable) {
                    /*
                     * This is a strange unknown scenario. In order to avoid false alarm, all variables to be analyzed
                     * need to be found and deleted according to the index.
                     */
                    deleteAnalysisByIndex(analysisMap, index);
                    continue;
                }
                LocalVariableAnalysis analysisInfo = analysisMap.get(localVariable);
                if ((analysisInfo == null) || (!analysisInfo.isRepeatFlag())) {
                    continue;
                }
                if (ins instanceof IINC || gotoTargets.contains(handle.getPosition())) {
                    analysisInfo.setRepeatFlag(false);
                    continue;
                }

                Location locationPrevious = locationList.get(i - 1);
                Instruction insPrevious = locationPrevious.getHandle().getInstruction();
                InsLoadConst preIns = parseInstruction(insPrevious, cpg);

                updateAnalysisByPreIns(analysisInfo, preIns);
            }
        }

        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        /*
         * The compiler sometimes optimizes a variable into two variables with the same name but different scopes of
         * action. This situation needs to be combined. If two variables have the same name and different values, no
         * alarm is required.
         */
        removalDup(analysisMap);

        for (Entry<LocalVariable, LocalVariableAnalysis> entry : analysisMap.entrySet()) {
            LocalVariableAnalysis analysisInfo = entry.getValue();
            if (!analysisInfo.isRepeatFlag()) {
                continue;
            }

            /*
             * String types are more often used in situations where long strings need to be used multiple times in a
             * method, and developers may not want to modify them (although constants are recommended in this case), so
             * confidence is changed to low.
             */
            LocalVariable localVariable = entry.getKey();
            int pri = NORMAL_PRIORITY;
            String signature = Utility.signatureToString(localVariable.getSignature(), false);
            if ("java.lang.String".equals(signature)) {
                pri = LOW_PRIORITY;
            }
            BugInstance bug = new BugInstance(this, "SPEC_LOCALVARIABLE_ASSIGNMENT_CHECK", pri);

            bug.addClassAndMethod(methodGen, sourceFile);
            bug.addString(localVariable.getName());
            SourceLineAnnotation sourceLine = SourceLineAnnotation.fromVisitedInstruction(classContext, method,
                    localVariable.getStartPC() - 1);
            bug.addSourceLine(sourceLine);

            bugReporter.reportBug(bug);
        }
    }

    /**
     * @param analysisMap
     * @param index
     */
    private static void deleteAnalysisByIndex(Map<LocalVariable, LocalVariableAnalysis> analysisMap, int index) {
        ArrayList<LocalVariable> needRemove = new ArrayList<>();
        for (LocalVariable key : analysisMap.keySet()) {
            if (key.getIndex() == index) {
                needRemove.add(key);
            }
        }

        for (LocalVariable key : needRemove) {
            analysisMap.remove(key);
        }

    }

    /**
     * @param analysisMap
     *            as you see
     */
    private static void removalDup(Map<LocalVariable, LocalVariableAnalysis> analysisMap) {
        HashMap<String, LocalVariableAnalysis> mapTmp = new HashMap<>();
        ArrayList<LocalVariable> needRemoveKeys = new ArrayList<>();
        for (Entry<LocalVariable, LocalVariableAnalysis> entry : analysisMap.entrySet()) {
            LocalVariable curVar = entry.getKey();
            LocalVariableAnalysis curInfo = entry.getValue();
            LocalVariableAnalysis dupInfo = mapTmp.putIfAbsent(curVar.getName(), curInfo);
            if (dupInfo != null) {
                needRemoveKeys.add(curVar);
                if (!curInfo.isRepeatFlag() || !Objects.equals(curInfo.getValue(), dupInfo.getValue())) {
                    dupInfo.setRepeatFlag(false);
                }
            }
        }

        for (LocalVariable key : needRemoveKeys) {
            analysisMap.remove(key);
        }

    }

    /**
     * When an instruction is written to a variable, the analysis result of the variable assignment is updated according
     * to the previous instruction. If the previous instruction is not a "load constant", then there is no problem of
     * repeatedly assigning the same constant to the variables being analyzed. If the constants of the previous
     * instruction are different from those of the current variable that has been assigned, there is no problem of
     * repeated assignment of the same constants for the analyzed variable.
     *
     * @param analysisInfo
     *            as you see
     * @param preIns
     *            Information on the previous instruction
     */
    private static void updateAnalysisByPreIns(LocalVariableAnalysis analysisInfo, InsLoadConst preIns) {
        if (!preIns.isLoadConst()) {
            analysisInfo.setRepeatFlag(false);
        } else {
            if (analysisInfo.isNotAssigned()) {
                analysisInfo.setValue(preIns.getConstValue());
            } else {
                if (!Objects.equals(analysisInfo.getValue(), preIns.getConstValue())) {
                    analysisInfo.setRepeatFlag(false);
                }
            }
        }
        analysisInfo.setNotAssigned(false);
    }

    /**
     * @param localVariableTable
     *            as you see
     * @return
     */
    private HashMap<LocalVariable, LocalVariableAnalysis> initAnalysisMap(LocalVariableTable localVariableTable) {
        HashMap<LocalVariable, LocalVariableAnalysis> analysisMap = new HashMap<>();
        LocalVariable[] tabs = localVariableTable.getLocalVariableTable();
        for (LocalVariable tab : tabs) {
            /* only local variables within the method are checked */
            if (tab.getStartPC() == 0) {
                continue;
            }

            /* Check only String and eight basic types of variables */
            String signature = Utility.signatureToString(tab.getSignature(), false);
            if (!analyseTypes.contains(signature)) {
                continue;
            }
            analysisMap.put(tab, new LocalVariableAnalysis());
        }

        return analysisMap;
    }

    /**
     * Resolve whether the instruction type is a "load constant" and the constant value
     */
    private static InsLoadConst parseInstruction(Instruction ins, ConstantPoolGen cpg) {
        InsLoadConst insLoadConst = new InsLoadConst();

        if (ins instanceof LDC) {
            LDC ldc = (LDC) ins;
            insLoadConst.setConstValue(ldc.getValue(cpg));
        } else if (ins instanceof LDC_W) {
            LDC_W ldc = (LDC_W) ins;
            insLoadConst.setConstValue(ldc.getValue(cpg));
        } else if (ins instanceof LDC2_W) {
            LDC2_W ldc = (LDC2_W) ins;
            insLoadConst.setConstValue(ldc.getValue(cpg));
        } else if (ins instanceof ACONST_NULL) {
            insLoadConst.setConstValue(null);
        } else if (ins instanceof ICONST) {
            ICONST iconst = (ICONST) ins;
            insLoadConst.setConstValue(iconst.getValue());
        } else if (ins instanceof LCONST) {
            LCONST lconst = (LCONST) ins;
            insLoadConst.setConstValue(lconst.getValue());
        } else if (ins instanceof FCONST) {
            FCONST fconst = (FCONST) ins;
            insLoadConst.setConstValue(fconst.getValue());
        } else if (ins instanceof DCONST) {
            DCONST dconst = (DCONST) ins;
            insLoadConst.setConstValue(dconst.getValue());
        } else if (ins instanceof BIPUSH) {
            BIPUSH bipush = (BIPUSH) ins;
            insLoadConst.setConstValue(bipush.getValue());
        } else if (ins instanceof SIPUSH) {
            SIPUSH sipush = (SIPUSH) ins;
            insLoadConst.setConstValue(sipush.getValue());
        } else {
            insLoadConst.setLoadConst(false);
        }

        return insLoadConst;
    }

    /**
     * Description:Judge if the ins is kind of StroreInstruction
     *
     * @param ins
     * @return
     */
    private static boolean neededAnaysis(Instruction ins) {
        if (ins instanceof StoreInstruction) {
            return true;
        }

        if (ins instanceof IINC) {
            return true;
        }

        return false;
    }

    /**
     * Description:Sort the locations according to their position
     *
     * @param iter
     * @return
     */
    private static ArrayList<Location> getSortedLocationIterator(Iterator<Location> iter) {
        TreeMap<Integer, Location> locationMap = new TreeMap<>();
        while (iter.hasNext()) {
            Location location = iter.next();
            Integer pc = Integer.valueOf(location.getHandle().getPosition());
            locationMap.put(pc, location);
        }
        ArrayList<Location> locationList = new ArrayList<>(locationMap.size());
        locationList.addAll(locationMap.values());
        return locationList;
    }

    @Override
    public void report() {
        /* nothing need to do */
    }

    /**
     * Store information about an instruction, is it load constant and the value of the constant
     */
    static class InsLoadConst {
        /** Is there an instruction to load constants */
        private boolean loadConst = true;

        /** Constant content, Number or String type */
        private Object constValue = null;

        /**
         * @return Returns the loadConst.
         */
        public boolean isLoadConst() {
            return loadConst;
        }

        /**
         * @param loadConst
         *            The loadConst to set.
         */
        public void setLoadConst(boolean loadConst) {
            this.loadConst = loadConst;
        }

        /**
         * @return Returns the constValue.
         */
        public Object getConstValue() {
            return constValue;
        }

        /**
         * @param constValue
         *            The constValue to set.
         */
        public void setConstValue(Object constValue) {
            this.constValue = constValue;
        }

        @Override
        public String toString() {
            return "InsLoadConst [loadConst=" + loadConst + ", constValue=" + constValue + "]";
        }
    }

    /**
     * Only analyse local variable which is String or eight basic types.
     */
    static class LocalVariableAnalysis {
        /** whether to assign values repeatedly in all branches */
        private boolean repeatFlag = true;

        /** Has it not been assigned yet */
        private boolean notAssigned = true;

        /** The last assigned object, Number or String */
        Object value = null;

        /**
         * @return Returns the repeatFlag.
         */
        public boolean isRepeatFlag() {
            return repeatFlag;
        }

        /**
         * @param repeatFlag
         *            The repeatFlag to set.
         */
        public void setRepeatFlag(boolean repeatFlag) {
            this.repeatFlag = repeatFlag;
        }

        /**
         * @return Returns the notAssigned.
         */
        public boolean isNotAssigned() {
            return notAssigned;
        }

        /**
         * @param notAssigned
         *            The notAssigned to set.
         */
        public void setNotAssigned(boolean notAssigned) {
            this.notAssigned = notAssigned;
        }

        /**
         * @return Returns the value.
         */
        public Object getValue() {
            return value;
        }

        /**
         * @param value
         *            The value to set.
         */
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LocalVariableAnalysis [repeatFlag=" + repeatFlag + ", notAssigned=" + notAssigned + ", value="
                    + value + "]";
        }
    }
}
