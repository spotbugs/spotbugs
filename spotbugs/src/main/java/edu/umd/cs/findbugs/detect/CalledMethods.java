/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.HashSet;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * Detector to find private methods that are never called.
 */
public class CalledMethods extends BytecodeScanningDetector implements NonReportingDetector {
    boolean emptyArrayOnTOS;

    HashSet<XField> emptyArray = new HashSet<>();

    HashSet<XField> nonEmptyArray = new HashSet<>();

    XFactory xFactory = AnalysisContext.currentXFactory();

    public CalledMethods(BugReporter bugReporter) {

    }

    /**
     * Records the methods targeted by method-reference handles ({@code Foo::bar}) in the
     * visited class's constant pool. Such targets appear only as {@code CONSTANT_MethodHandle}
     * constants, never as {@code invoke*} opcodes, so {@link #sawOpcode(int)} cannot see them;
     * without this they would look uncalled and cause UMAC false positives.
     *
     * @param obj the class whose constant pool is scanned
     */
    @Override
    public void visit(JavaClass obj) {
        ConstantPool cp = obj.getConstantPool();
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        for (Constant constant : cp.getConstantPool()) {
            if (!(constant instanceof ConstantMethodHandle)) {
                continue;
            }
            ConstantMethodHandle method = (ConstantMethodHandle) constant;
            int kind = method.getReferenceKind();
            if (kind < Const.REF_invokeVirtual || kind > Const.REF_invokeInterface) {
                continue;
            }
            Constant ref = cp.getConstant(method.getReferenceIndex());
            if (!(ref instanceof ConstantCP)) {
                continue;
            }
            ConstantCP refCP = (ConstantCP) ref;
            String slashedClassName = cp.getConstantString(refCP.getClassIndex(), Const.CONSTANT_Class);
            if (slashedClassName.charAt(0) == '[') {
                continue;
            }
            ClassDescriptor c = DescriptorFactory.instance().getClassDescriptor(slashedClassName);
            if (!subtypes2.isApplicationClass(c)) {
                continue;
            }
            ConstantNameAndType nameAndType = cp.getConstant(refCP.getNameAndTypeIndex());
            xFactory.addCalledMethod(DescriptorFactory.instance().getMethodDescriptor(slashedClassName,
                    nameAndType.getName(cp), nameAndType.getSignature(cp), kind == Const.REF_invokeStatic));
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC)) {
            XField f = getXFieldOperand();
            if (f != null && (f.isFinal() || !f.isProtected() && !f.isPublic())) {
                if (emptyArrayOnTOS) {
                    emptyArray.add(f);
                } else {
                    nonEmptyArray.add(f);
                }
            }
        }
        emptyArrayOnTOS = (seen == Const.ANEWARRAY || seen == Const.NEWARRAY || seen == Const.MULTIANEWARRAY && getIntConstant() == 1)
                && getPrevOpcode(1) == Const.ICONST_0;

        if (seen == Const.GETSTATIC || seen == Const.GETFIELD) {
            XField f = getXFieldOperand();
            if (emptyArray.contains(f) && !nonEmptyArray.contains(f) && f.isFinal()) {
                emptyArrayOnTOS = true;
            }
        }
        switch (seen) {
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
        case Const.INVOKEINTERFACE:
            ClassDescriptor c = getClassDescriptorOperand();
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            if (subtypes2.isApplicationClass(c)) {
                xFactory.addCalledMethod(getMethodDescriptorOperand());
            }

            break;
        default:
            break;
        }
    }

    @Override
    public void report() {
        emptyArray.removeAll(nonEmptyArray);
        for (XField f : emptyArray) {
            xFactory.addEmptyArrayField(f);
        }
        emptyArray.clear();
    }

}
