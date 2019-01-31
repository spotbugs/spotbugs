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
    private final BugAccumulator bugAccumulator;
    private final BugReporter bugReporter;

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
        // get the key object class name of Map, or object class name of Set
        String keyClassName = getObjInMapOrSet(field);

        // check the object has rewritten equals and hashCode methods
        boolean res = checkObjEuqalAndHashCode(keyClassName);

        if (!res) {
            fillFieldWarningReport(field, classContext.getJavaClass().getClassName());
        }
    }

    /**
     * Get key object in Map or object in Set
     *
     * @param field
     *            field
     * @return key object in Map or object in Set
     */
    private String getObjInMapOrSet(Field field) {
        String keyClassName = null;
        Type type = field.getType();

        if (!(type instanceof ObjectType)) {
            return keyClassName;
        }

        ObjectType objType = (ObjectType) type;

        // get the field class name
        String className = objType.getClassName();

        // if the field is Set or Map, continue
        if (!"java.util.Set".equals(className) && !"java.util.Map".equals(className)) {
            return keyClassName;
        }

        // generic signature of field, for example: Ljava/util/Set<Lcom/h3c/spotbugs/test/TestObject;>;
        String sig = field.getGenericSignature();

        return getObjClassOrInerClassFromStr(false, sig);
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

        // constant pool of this method
        ConstantPoolGen constPool = classContext.getConstantPoolGen();

        // location list (instruction list)
        Collection<Location> locationCollection = cfg.orderedLocations();

        ArrayList<Location> locationList = new ArrayList<>();
        locationList.addAll(locationCollection);

        checkLocalVariableInMethod(method, classContext);

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
                String methodName = getClassOrMethodFromInstruction(false, ((INVOKEINTERFACE) ins).getIndex(), constPool);

                if ("java/util/List".equals(className) && "remove".equals(methodName)) {
                    String removeObj = getObjectInList(location, classContext, method);

                    checkObjImplEqual(removeObj, location, classContext, method);

                }
            }
        }
    }

    /**
     * Check the local variable in method. If there are variables defined as Map or Set, check whether the key object
     * has rewritten equals and hashCode methods.
     *
     * @param method
     *            method
     * @param classContext
     *            class context
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void checkLocalVariableInMethod(Method method, ClassContext classContext)
            throws DataflowAnalysisException, CFGBuilderException {
        LocalVariableTypeTable localTypeTable = classContext.getMethodGen(method).getLocalVariableTypeTable();

        if (null == localTypeTable) {
            return;
        }

        // get local variable type array
        LocalVariable[] typeArray = localTypeTable.getLocalVariableTypeTable();

        for (LocalVariable local : typeArray) {

            // start pc ==0 means it is this or incoming parameter, ignore it
            if (0 == local.getStartPC()) {
                continue;
            }

            String sig = local.getSignature();
            String localValuClass = getObjClassOrInerClassFromStr(true, sig);

            if (!"java/util/Set".equals(localValuClass) && !"java/util/Map".equals(localValuClass)) {
                continue;
            }

            String KeyClassName = getObjClassOrInerClassFromStr(false, sig);

            boolean res = checkObjEuqalAndHashCode(KeyClassName);

            if (!res) {
                fillLocalWarnReport(local, classContext, method);
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
     * check whether the object has rewritten the equals method
     *
     * @param className
     *            class name
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private void checkObjImplEqual(String className, Location location, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == className) {
            return;
        }

        if ("java/lang/Class".equals(className)) {
            return;
        }

        JavaClass objClass = null;

        try {
            objClass = Repository.lookupClass(className);
        } catch (ClassNotFoundException e) {
            return;
        }

        // if the object is Interface, Abstract class or Enum, ignore it
        if (objClass.isInterface() || objClass.isAbstract() || objClass.isEnum()) {
            return;
        }
        Method[] methods = objClass.getMethods();

        for (Method methodTmp : methods) {
            if ("equals".equals(methodTmp.getName())) {
                return;
            }
        }

        fillListRemoveReport(location, classContext, method);
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
                new BugInstance(this, "SPEC_LIST_OBJECT_NO_EQUALS", NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                sourceLineAnnotation);
    }

    /**
     * Fill the bug report
     *
     * @param localValue
     *            local variable
     * @param classContext
     *            class context
     * @param method
     *            method
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    private void fillLocalWarnReport(LocalVariable localValue, ClassContext classContext, Method method)
            throws DataflowAnalysisException, CFGBuilderException {
        if (null == localValue) {
            return;
        }

        BugInstance bugInstance = new BugInstance(this, "SPEC_MAP_SET_OBJECT_NO_EQUALS_HASHCODE", NORMAL_PRIORITY);
        bugInstance.addClassAndMethod(classContext.getJavaClass(), method);
        bugInstance.addString(localValue.getName());
        bugInstance.addSourceLine(
                SourceLineAnnotation.fromVisitedInstruction(classContext, method, localValue.getStartPC() - 1));

        bugReporter.reportBug(bugInstance);
    }

    /**
     * Fill the field warning report
     *
     * @param field
     *            field
     * @param classInfo
     *            class information
     */
    private void fillFieldWarningReport(Field filed, String className) {
        FieldAnnotation fieldAnnotation = new FieldAnnotation(className, filed.getName(), filed.getSignature(),
                filed.isStatic());

        bugReporter.reportBug(new BugInstance(this, "SPEC_MAP_SET_OBJECT_NO_EQUALS_HASHCODE", NORMAL_PRIORITY)
                .addClass(className).addField(fieldAnnotation));
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}
