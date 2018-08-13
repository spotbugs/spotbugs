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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DSTORE;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FSTORE;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.StoreInstruction;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
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
public class LocalVariableAssignmentCheck implements Detector {

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private final static String STRING_FLAG = "Not a push action";

    public LocalVariableAssignmentCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            String methodName = method.getName();

            // If Constrcutor or hashCode method,skip
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName) || "hashCode".equals(methodName)) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (Exception e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }

            bugAccumulator.reportAccumulatedBugs();
        }
    }

    /**
     * @param classContext
     * @param method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        LineNumberTable lineNumberTable = method.getLineNumberTable();
        int variableCount = localVariableTable.getTableLength();
        // The variable count of the method is 0,return
        if (variableCount == 0) {
            return;
        }

        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        // Sort the location according to their position at the class file
        Iterator<Location> iter = cfg.locationIterator();
        ArrayList<Location> locationList = getSortedLocationIterator(iter);

        // The map stores the message of the local variables
        HashMap<LocalVariable, LocalVariableStoreMessage> variableStoreMap = new HashMap<>();

        for (int i = 1; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            // ISTORE、LSTORE、FSTORE、DSTORE
            if (neededAction(ins)) {
                StoreInstruction store = (StoreInstruction) ins;
                int index = store.getIndex();
                LocalVariable local = localVariableTable.getLocalVariable(index);

                if (null == local) {
                    continue;
                }

                if (local.getStartPC() == 0) {
                    continue;
                }

                Location locationPrevious = locationList.get(i - 1);
                Instruction insPrevious = locationPrevious.getHandle().getInstruction();
                String insValue = basicTypeLoad(insPrevious, cpg);
                LocalVariableStoreMessage storeMessage = variableStoreMap.get(local);

                // If there are two actions has the same sourceLine with current one,
                // it means the value was loaded from an expression.
                boolean valueLoadedFromExpression = false;
                if (i >= 2) {
                    Location pprev = locationList.get(i - 2);
                    int srcLineCur = lineNumberTable.getSourceLine(location.getHandle().getPosition());
                    int srcLinePrev = lineNumberTable.getSourceLine(locationPrevious.getHandle().getPosition());
                    int srcLinePPrev = lineNumberTable.getSourceLine(pprev.getHandle().getPosition());
                    if (srcLineCur == srcLinePrev && srcLineCur == srcLinePPrev) {
                        valueLoadedFromExpression = true;
                    }
                }

                // Store the variable message to the map if the first store action,otherwise judge if the previous
                // action is kind of push action.
                if (null == storeMessage) {
                    variableStoreMap.put(local, new LocalVariableStoreMessage(local));
                    storeMessage = variableStoreMap.get(local);

                    if (STRING_FLAG.equals(insValue) || valueLoadedFromExpression) {
                        storeMessage.setbIsSameValue(false);
                        continue;
                    }

                    storeMessage.setValue(insValue);
                    storeMessage.setLocation(location);
                } else {
                    if (!storeMessage.isbIsSameValue()) {
                        continue;
                    }

                    if (STRING_FLAG.equals(insValue) || valueLoadedFromExpression) {
                        storeMessage.setbIsSameValue(false);
                        continue;
                    }

                    if (null != insValue && !insValue.equals(storeMessage.getValue())) {
                        storeMessage.setbIsSameValue(false);
                        continue;
                    }

                    storeMessage.setLocation(location);
                }
            }

            // IINC
            if (ins instanceof IINC) {
                IINC iinc = (IINC) ins;
                int index = iinc.getIndex();
                LocalVariable local = localVariableTable.getLocalVariable(index, location.getHandle().getPosition());

                if (null == local) {
                    continue;
                }

                if (local.getStartPC() == 0) {
                    continue;
                }

                LocalVariableStoreMessage storeMessage = variableStoreMap.get(local);

                if (null != storeMessage) {
                    storeMessage.setbIsSameValue(false);
                }
            }
        }

        // Judge which variable which stored in the map need to report bug.
        for (LocalVariableStoreMessage varStoreMessage : variableStoreMap.values()) {
            if (varStoreMessage.isbIsSameValue()) {
                Location varLocation = varStoreMessage.getLocation();
                InstructionHandle handle = varLocation.getHandle();
                int position = handle.getPosition();
                int index = varStoreMessage.getVariable().getIndex();
                String varName = varStoreMessage.getVariable().getName();
                int line = lineNumberTable.getSourceLine(position);
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext,
                        methodGen, sourceFile, handle);

                BugAnnotation variableAnnotation = new LocalVariableAnnotation(varName, index, position, line);
                variableAnnotation.setDescription("LOCAL_VARIABLE_VALUE_OF");
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "SPEC_LOCALVARIABLE_ASSIGNMENT_CHECK", HIGH_PRIORITY)
                                .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                        sourceLineAnnotation);
            }
        }
    }

    /**
     * Description Judge if the ins is kind of PushInstruction
     *
     * @param ins
     * @return
     */
    private String basicTypeLoad(Instruction ins, ConstantPoolGen cpg) {

        if (ins instanceof LDC) {
            LDC ldc = (LDC) ins;
            return ldc.getValue(cpg).toString();
        }

        if (ins instanceof ICONST) {
            ICONST iconst = (ICONST) ins;
            return iconst.getValue().toString();
        }

        if (ins instanceof LCONST) {
            LCONST lconst = (LCONST) ins;
            return lconst.getValue().toString();
        }

        if (ins instanceof FCONST) {
            FCONST fconst = (FCONST) ins;
            return fconst.getValue().toString();
        }

        if (ins instanceof DCONST) {
            DCONST dconst = (DCONST) ins;
            return dconst.getValue().toString();
        }

        if (ins instanceof BIPUSH) {
            BIPUSH bipush = (BIPUSH) ins;
            return bipush.getValue().toString();
        }

        if (ins instanceof SIPUSH) {
            SIPUSH sipush = (SIPUSH) ins;
            return sipush.getValue().toString();
        }

        return STRING_FLAG;
    }

    /**
     * Description:Judge if the ins is kind of StroreInstruction
     *
     * @param ins
     * @return
     */
    private boolean neededAction(Instruction ins) {
        if (ins instanceof ISTORE) {
            return true;
        }

        if (ins instanceof LSTORE) {
            return true;
        }

        if (ins instanceof FSTORE) {
            return true;
        }

        if (ins instanceof DSTORE) {
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
    private ArrayList<Location> getSortedLocationIterator(Iterator<Location> iter) {
        ArrayList<Location> locationList = new ArrayList<>();
        TreeMap<Integer, Location> locationMap = new TreeMap<>();
        while (iter.hasNext()) {
            Location location = iter.next();
            int pc = location.getHandle().getPosition();
            locationMap.put(pc, location);
        }
        locationList.addAll(locationMap.values());
        return locationList;
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

    /**
     * Description:Class used to store the message of the local variables of the method to be detected.
     */
    class LocalVariableStoreMessage {
        private LocalVariable variable;
        private boolean bIsSameValue = true;
        private Location location;
        private String value;

        public LocalVariable getVariable() {
            return variable;
        }

        public void setVariable(LocalVariable variable) {
            this.variable = variable;
        }

        public boolean isbIsSameValue() {
            return bIsSameValue;
        }

        public void setbIsSameValue(boolean bIsSameValue) {
            this.bIsSameValue = bIsSameValue;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        LocalVariableStoreMessage(LocalVariable variable) {
            this.variable = variable;
        }
    }
}
