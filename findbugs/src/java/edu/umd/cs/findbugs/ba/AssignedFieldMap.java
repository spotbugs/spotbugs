/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;

public class AssignedFieldMap implements Constants {
    /*
     * private final Map<Method, Set<XField>> assignedFieldSetForMethodMap;
     * private final JavaClass myClass;
     *
     * public AssignedFieldMap(JavaClass jclass) {
     * this.assignedFieldSetForMethodMap = new IdentityHashMap<Method,
     * Set<XField>>(); this.myClass = jclass; }
     *
     * public void build() throws ClassNotFoundException { // Build a set of all
     * fields that could be assigned // by methods in this class Set<XField>
     * assignableFieldSet = new HashSet<XField>(); scanFields(myClass,
     * assignableFieldSet); JavaClass[] superClassList =
     * myClass.getSuperClasses(); if (superClassList != null) { for (JavaClass
     * aSuperClassList : superClassList) { scanFields(aSuperClassList,
     * assignableFieldSet); } }
     *
     * Method[] methodList = myClass.getMethods(); for (Method method :
     * methodList) {
     *
     * scanMethod(method, assignableFieldSet); } }
     *
     * public Set<XField> getAssignedFieldSetForMethod(Method method) {
     * Set<XField> set = assignedFieldSetForMethodMap.get(method); if (set ==
     * null) { set = new HashSet<XField>();
     * assignedFieldSetForMethodMap.put(method, set); } return set; }
     *
     * private void scanFields(JavaClass jclass, Set<XField> assignableFieldSet)
     * { // JavaClass myClass = classContext.getJavaClass(); String myClassName
     * = myClass.getClassName(); String myPackageName =
     * myClass.getPackageName();
     *
     * String superClassName = jclass.getClassName(); String superPackageName =
     * jclass.getPackageName();
     *
     * Field[] fieldList = jclass.getFields(); for (Field field : fieldList) {
     * if (field.isStatic()) continue; boolean assignable; if (field.isPublic()
     * || field.isProtected()) assignable = true; else if (field.isPrivate())
     * assignable = myClassName.equals(superClassName); else // package
     * protected assignable = myPackageName.equals(superPackageName);
     *
     * if (assignable) { assignableFieldSet.add(new
     * InstanceField(superClassName, field.getName(), field.getSignature(),
     * field.getAccessFlags())); } } }
     *
     * private void scanMethod(Method method, Set<XField> assignableFieldSet)
     * throws ClassNotFoundException { //MethodGen methodGen =
     * classContext.getMethodGen(method);
     *
     * MethodGen methodGen; try { methodGen=
     * Global.getAnalysisCache().getMethodAnalysis(MethodGen.class,
     * BCELUtil.getMethodDescriptor(myClass, method)); } catch
     * (CheckedAnalysisException e) { // Should not happen throw new
     * AnalysisException("Could not get MethodGen for Method", e); }
     *
     * if (methodGen == null) return; InstructionList il =
     * methodGen.getInstructionList(); InstructionHandle handle = il.getStart();
     *
     * ConstantPoolGen cpg = methodGen.getConstantPool();
     *
     * while (handle != null) { Instruction ins = handle.getInstruction(); short
     * opcode = ins.getOpcode(); if (opcode == Constants.PUTFIELD) { PUTFIELD
     * putfield = (PUTFIELD) ins;
     *
     * XField instanceField = Hierarchy.findXField(putfield, cpg); if
     * (instanceField != null && assignableFieldSet.contains(instanceField)) {
     * Set<XField> assignedFieldSetForMethod =
     * getAssignedFieldSetForMethod(method);
     * assignedFieldSetForMethod.add(instanceField); } }
     *
     * handle = handle.getNext(); } }
     */
}

