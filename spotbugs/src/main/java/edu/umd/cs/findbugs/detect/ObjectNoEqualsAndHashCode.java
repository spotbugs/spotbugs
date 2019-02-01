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
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTypeTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

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
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;

/**
 * @since ?
 *
 */
public class ObjectNoEqualsAndHashCode implements Detector {

    private final static List<String> mapFilterMethods;

    private final static List<String> setFilterMethods;

    private final BugAccumulator bugAccumulator;
    private final BugReporter bugReporter;

    static {
        mapFilterMethods = new ArrayList<>();
        mapFilterMethods.add("size");
        mapFilterMethods.add("isEmpty");
        mapFilterMethods.add("clear");
        mapFilterMethods.add("keySet");
        mapFilterMethods.add("values");
        mapFilterMethods.add("entrySet");

        setFilterMethods = new ArrayList<>();
        setFilterMethods.add("size");
        setFilterMethods.add("isEmpty");
        setFilterMethods.add("iterator");
        setFilterMethods.add("clear");
        setFilterMethods.add("spliterator");
        setFilterMethods.add("toArray");
    }

    public ObjectNoEqualsAndHashCode(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Field[] fields = classContext.getJavaClass().getFields();
        // ConstantPoolGen constPool = classContext.getConstantPoolGen();
        for (Field field : fields) {
            analyzeField(field, classContext);
        }

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
     * Analyze the field, if class is Set or Map, the object stored in it must rewrite euqals and hashCode
     *
     * @param field
     *            field to be check
     */
    private void analyzeField(Field field, ClassContext classContext) {
        Type type = field.getType();

        if (!(type instanceof ObjectType)) {
            return;
        }

        ObjectType objType = (ObjectType) type;

        // get the field class name
        String className = objType.getClassName();

        // if the field is not Set or Map, return
        if (!"java.util.Set".equals(className) && !"java.util.Map".equals(className)) {
            return;
        }

        // generic signature of field, for example: Ljava/util/Set<Lcom/h3c/spotbugs/test/TestObject;>;
        String sig = field.getGenericSignature();

        // get the key object class name of Map, or object class name of Set
        String keyClassName = getObjClassOrInerClassFromStr(false, sig);

        // check the object has rewritten equals and hashCode methods
        boolean res = checkObjEuqalAndHashCode(keyClassName);

        if (!res) {
            if ("java.util.Map".equals(className)) {
                fillFieldWarningReport(true, field, classContext.getJavaClass().getClassName());
            } else if ("java.util.Set".equals(className)) {
                fillFieldWarningReport(false, field, classContext.getJavaClass().getClassName());
            }
        }
    }

    /**
     * Get the Object class, or the key object class in map or object inSet.
     *
     * <blockquote>For example,
     *
     * <pre>
     * {@code
     *     signature = "Ljava/util/Set<Lcom/h3c/spotbugs/test/TestObject;>;"
     *     flag==true: return "java/util/Set"
     *     flag==false: return "com/h3c/spotbugs/test/TestObject"
     * }
     * </pre>
     *
     * </blockquote>
     *
     * @param flag
     *            true: return object class; false: return key object class in map or object inSet
     * @param signature
     *            signature
     * @return object class
     */
    private String getObjClassOrInerClassFromStr(boolean flag, String signature) {
        String className = null;

        String[] tmp = signature.split("<");
        if (tmp.length >= 2) {
            if (flag) {
                String name = tmp[0];
                if (name.length() >= 1) {
                    className = name.substring(1, name.length());
                }
            } else {
                String objSource = tmp[1];
                String[] keyValue = objSource.split(";");
                if (keyValue.length >= 1) {
                    className = keyValue[0];
                    if (className.length() > 1) {
                        className = className.substring(1, className.length());
                    }
                }
            }

        }

        return className;
    }

    /**
     * Check whether the object rewrite the equals and hashCode methods
     *
     * @param keyClassName
     *            the object class name in map key or set
     * @return boolean true: rewrite; false: not rewrite
     */
    private boolean checkObjEuqalAndHashCode(String keyClassName) {
        boolean isEqual = false;
        boolean isHashCode = false;

        if (null == keyClassName) {
            return true;
        }

        if ("java/lang/Class".equals(keyClassName)) {
            return true;
        }

        JavaClass objClass = null;
        try {
            objClass = Repository.lookupClass(keyClassName);
        } catch (ClassNotFoundException e) {
            return true;
        }

        if (objClass.isInterface() || objClass.isAbstract() || objClass.isEnum()) {
            return true;
        }

        Method[] methods = objClass.getMethods();

        for (Method method : methods) {
            if ("equals".equals(method.getName())) {
                isEqual = true;
            } else if ("hashCode".equals(method.getName())) {
                isHashCode = true;
            }

            if (isEqual && isHashCode) {
                break;
            }
        }

        if (isEqual && isHashCode) {
            return true;
        }

        return false;
    }

    /**
     * Analyze the method, the local variable in method and list.remove(object)
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

        // checkLocalVariableInMethod(method, classContext);
        List<LocalVariable> mapVariables = new ArrayList<>();
        List<LocalVariable> setVariables = new ArrayList<>();
        getMapSetLocalVariale(method, classContext, mapVariables, setVariables);

        ConstantPoolGen constPool = classContext.getConstantPoolGen();
        // location list (instruction list)
        Collection<Location> locationCollection = cfg.orderedLocations();
        ArrayList<Location> locationList = new ArrayList<>();
        locationList.addAll(locationCollection);

        for (int i = 0; i < locationList.size(); i++) {
            Location location = locationList.get(i);
            InstructionHandle insHandle = location.getHandle();

            if (null == insHandle) {
                continue;
            }

            Instruction ins = insHandle.getInstruction();

            // bytecode: invokeinterface #{position} //InterfaceMethod java/util/List.remove(Object)
            if (ins instanceof INVOKEINTERFACE) {
                String className = getClassOrMethodFromInstruction(true, ((INVOKEINTERFACE) ins).getIndex(), constPool);
                String methodName = getClassOrMethodFromInstruction(false, ((INVOKEINTERFACE) ins).getIndex(),
                        constPool);

                if ("java/util/List".equals(className) && "remove".equals(methodName)) {
                    String removeObj = getObjectInList(location, classContext, method);
                    boolean res = checkObjEuqalAndHashCode(removeObj);

                    if (!res) {
                        fillListRemoveReport(location, classContext, method);
                    }
                    continue;
                }

                String localName = getObjectNameFromInstruction(location, classContext, method);
                if (null == localName) {
                    continue;
                }

                if ("java/util/Map".equals(className)) {
                    if (mapVariables.isEmpty()) {
                        continue;
                    }

                    if (mapFilterMethods.contains(methodName)) {
                        continue;
                    }

                    Iterator<LocalVariable> iterator = mapVariables.iterator();
                    while (iterator.hasNext()) {
                        LocalVariable value = iterator.next();

                        if (localName.equals(value.getName())) {
                            int range = value.getStartPC() + value.getLength();
                            /*
                             * There may be multiple variables with the same name in one method, therefore apply the
                             * start pc value to distinguish them.
                             */
                            if (insHandle.getPosition() > value.getStartPC() && insHandle.getPosition() < range) {
                                fillLocalWarnReport(true, value, classContext, method);
                                iterator.remove();
                                break;
                            }

                        }
                    }

                    continue;
                }

                if ("java/util/Set".equals(className)) {
                    if (setVariables.isEmpty()) {
                        continue;
                    }

                    if (setFilterMethods.contains(methodName)) {
                        continue;
                    }

                    Iterator<LocalVariable> iterator = setVariables.iterator();
                    while (iterator.hasNext()) {
                        LocalVariable value = iterator.next();

                        if (localName.equals(value.getName())) {
                            int range = value.getStartPC() + value.getLength();
                            if (insHandle.getPosition() > value.getStartPC() && insHandle.getPosition() < range) {
                                fillLocalWarnReport(false, value, classContext, method);
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the name of the object which called method. For example, instruction: testMap.get(map); testMap is the
     * object's name
     *
     * @param location
     *            instruction location
     * @param classContext
     *            class context
     * @param method
     *            method
     * @return name of the object
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private String getObjectNameFromInstruction(Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAtLocation(location);

        if (vnaFrame.getNumSlots() < 2) {
            return null;
        }
        // If there is parameter，the object is the second last in stack
        ValueNumber valueNumber = vnaFrame.getValue(vnaFrame.getNumSlots() - 2);
        // local variable
        LocalVariableAnnotation localAnn = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location,
                valueNumber, vnaFrame);
        if (null != localAnn) {
            return localAnn.getName();
        }

        return null;
    }


    /**
     * Get local variables of Map and Set in the method, and add these variables to list The key of Map doesn't
     * rewritten equals and hashCode methods. The object stored in Set doesn't rewritten equals and hashCode methods.
     * 
     * @param method
     *            method
     * @param classContext
     *            class context
     * @param mapVariables
     *            Map variables
     * @param setVariables
     *            Set variables
     */
    private void getMapSetLocalVariale(Method method, ClassContext classContext, List<LocalVariable> mapVariables,
            List<LocalVariable> setVariables) {
        LocalVariableTypeTable localTypeTable = classContext.getMethodGen(method).getLocalVariableTypeTable();

        if (null == localTypeTable) {
            return;
        }

        // get local variable type array
        LocalVariable[] typeArray = localTypeTable.getLocalVariableTypeTable();

        for (LocalVariable local : typeArray) {
            String sig = local.getSignature();
            String localValueClass = getObjClassOrInerClassFromStr(true, sig);

            if (!"java/util/Set".equals(localValueClass) && !"java/util/Map".equals(localValueClass)) {
                continue;
            }

            String KeyClassName = getObjClassOrInerClassFromStr(false, sig);

            boolean res = checkObjEuqalAndHashCode(KeyClassName);

            if (!res) {
                if ("java/util/Map".equals(localValueClass)) {
                    mapVariables.add(local);
                } else if ("java/util/Set".equals(localValueClass)) {
                    setVariables.add(local);
                }
            }
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
    private String getClassOrMethodFromInstruction(boolean flag, int constIndex, ConstantPoolGen constPool) {
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
     * Get the object class name stored in list
     *
     * @param location
     *            the location of instruction
     * @param classContext
     *            class context
     * @param method
     *            method
     * @return String object class name
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private String getObjectInList(Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
        Type topType = typeFrame.getTopValue();
        String className = null;

        if (topType instanceof ObjectType) {
            ObjectType objType = (ObjectType) topType;
            className = objType.getClassName();
        }

        return className;
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
    private void fillListRemoveReport(Location location, ClassContext classContext, Method method)
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
                new BugInstance(this, "SPEC_OBJECT_NO_EQUALS_HASHCODE_LIST", NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                sourceLineAnnotation);
    }

    /**
     * Fill the bug report
     *
     * @param isMap
     *            true: map warning, false: set warning
     * @param localValue
     *            local variable
     * @param classContext
     *            class context
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void fillLocalWarnReport(boolean isMap, LocalVariable localValue, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == localValue) {
            return;
        }
        BugInstance bugInstance;

        if (isMap) {
            bugInstance = new BugInstance(this, "SPEC_OBJECT_NO_EQUALS_HASHCODE_MAP", NORMAL_PRIORITY);
        } else {
            bugInstance = new BugInstance(this, "SPEC_OBJECT_NO_EQUALS_HASHCODE_SET", NORMAL_PRIORITY);
        }

        bugInstance.addClassAndMethod(classContext.getJavaClass(), method);
        bugInstance.addString(localValue.getName());
        int pc = localValue.getStartPC();
        if (pc > 1) {
            pc = pc - 1;
        }
        bugInstance.addSourceLine(
                SourceLineAnnotation.fromVisitedInstruction(classContext, method, localValue.getStartPC() - 1));

        bugReporter.reportBug(bugInstance);
    }

    /**
     * Fill the field warning report
     *
     * @param isMap
     *            true: map warning, false: set warning
     * @param field
     *            field
     * @param classInfo
     *            class information
     */
    private void fillFieldWarningReport(boolean isMap, Field filed, String className) {
        FieldAnnotation fieldAnnotation = new FieldAnnotation(className, filed.getName(), filed.getSignature(),
                filed.isStatic());

        if (isMap) {
            bugReporter.reportBug(new BugInstance(this, "SPEC_OBJECT_NO_EQUALS_HASHCODE_MAP", NORMAL_PRIORITY)
                    .addClass(className).addField(fieldAnnotation));
        } else {
            bugReporter.reportBug(new BugInstance(this, "SPEC_OBJECT_NO_EQUALS_HASHCODE_SET", NORMAL_PRIORITY)
                    .addClass(className).addField(fieldAnnotation));
        }
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}
