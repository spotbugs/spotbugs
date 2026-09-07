/*
 * SpotBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGenException;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Utility methods for working with bootstrap methods
 *
 * @author Ádám Balogh
 */
public class BootstrapMethodsUtil {

    private static final String LAMBDA_METHOD_PREFIX = "lambda$";

    /**
     * Returns the methods invoked by handle arguments of a bootstrap method from a Java class.
     *
     * @param cls            the java class whose bootstrap method attribute is consulted
     * @param bootstrapIndex the index of the bootstrap method
     * @param cp             the constant pool of the java class
     * @return the invoked methods if found, an empty list otherwise
     */
    public static List<MethodDescriptor> getInvokedMethodTargets(JavaClass cls, int bootstrapIndex, ConstantPool cp) {
        BootstrapMethod bm = findBootstrapMethod(cls, bootstrapIndex);
        if (bm == null) {
            return Collections.emptyList();
        }
        List<MethodDescriptor> targets = new ArrayList<>();
        for (int arg : bm.getBootstrapArguments()) {
            Constant constant = cp.getConstant(arg);
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
            ConstantNameAndType nameAndType = cp.getConstant(refCP.getNameAndTypeIndex());
            targets.add(DescriptorFactory.instance().getMethodDescriptor(slashedClassName,
                    nameAndType.getName(cp), nameAndType.getSignature(cp), kind == Const.REF_invokeStatic));
        }
        return targets;
    }

    private static BootstrapMethod findBootstrapMethod(JavaClass cls, int index) {
        for (Attribute attr : cls.getAttributes()) {
            if (attr instanceof BootstrapMethods) {
                return ((BootstrapMethods) attr).getBootstrapMethods()[index];
            }
        }
        return null;
    }

    /**
     * Returns the method representation of a bootstrap method from a Java class.
     *
     * @param bms
     *      the BootstrapMethods attribute of a java class
     * @param index
     *      the index of the bootstrap method
     * @param cp
     *      the constant pool of the java class
     * @param cls
     *      the java class itself
     * @return the bootstrap method represented as Method if found, Optional.empty() otherwise
     */
    public static Optional<Method> getMethodFromBootstrap(BootstrapMethods bms, int index, ConstantPool cp, JavaClass cls) {
        BootstrapMethod bm = bms.getBootstrapMethods()[index];
        for (int arg : bm.getBootstrapArguments()) {
            Constant c = bms.getConstantPool().getConstant(arg);
            if (!(c instanceof ConstantMethodHandle)) {
                continue;
            }
            ConstantMethodHandle cmh = (ConstantMethodHandle) c;
            c = cp.getConstant(cmh.getReferenceIndex());
            if (!(c instanceof ConstantMethodref) && !(c instanceof ConstantInterfaceMethodref)) {
                continue;
            }
            ConstantCP ccp = (ConstantCP) c;
            if (ccp.getClassIndex() != cls.getClassNameIndex()) {
                return Optional.empty();
            }
            ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(ccp.getNameAndTypeIndex());
            Optional<Method> metOpt = Arrays.stream(cls.getMethods())
                    .filter(m -> m.getNameIndex() == cnat.getNameIndex()
                            && m.getSignatureIndex() == cnat.getSignatureIndex())
                    .findAny();
            if (metOpt.isEmpty()) {
                continue;
            }
            return metOpt;
        }
        return Optional.empty();
    }

    /**
     * Returns the lambda methods belonging to the given method (including nested ones).
     *
     * @param cls    the java class containing the method
     * @param method the method whose lambdas to find
     * @return the lambda methods if found, an empty set otherwise
     */
    public static Set<MethodAnnotation> getLambdasOwnedBy(JavaClass cls, Method method) {
        Set<MethodAnnotation> foundLambdas = new HashSet<>();
        Queue<Method> methodsToCheck = new ArrayDeque<>();
        methodsToCheck.add(method);
        while (!methodsToCheck.isEmpty()) {
            for (MethodAnnotation lambda : getDirectlyReferencedLambdas(cls, methodsToCheck.remove())) {
                if (foundLambdas.add(lambda)) {
                    Arrays.stream(cls.getMethods())
                            .filter(m -> m.getName().equals(lambda.getMethodName()) && m.getSignature().equals(lambda.getMethodSignature()))
                            .forEach(methodsToCheck::add);
                }
            }
        }
        return foundLambdas;
    }

    private static List<MethodAnnotation> getDirectlyReferencedLambdas(JavaClass cls, Method method) {
        Code code = method.getCode();
        if (code == null) {
            return Collections.emptyList();
        }
        InstructionList instructions;
        try {
            instructions = new InstructionList(code.getCode());
        } catch (ClassGenException e) {
            return Collections.emptyList();
        }
        String slashedClassName = ClassName.toSlashedClassName(cls.getClassName());
        ConstantPool cp = cls.getConstantPool();
        List<MethodAnnotation> lambdas = new ArrayList<>();
        for (InstructionHandle handle : instructions) {
            Instruction instruction = handle.getInstruction();
            if (!(instruction instanceof INVOKEDYNAMIC)) {
                continue;
            }
            ConstantInvokeDynamic constDyn = cp.getConstant(((INVOKEDYNAMIC) instruction).getIndex());
            for (MethodDescriptor target : getInvokedMethodTargets(cls, constDyn.getBootstrapMethodAttrIndex(), cp)) {
                if (slashedClassName.equals(target.getSlashedClassName()) && target.getName().startsWith(LAMBDA_METHOD_PREFIX)) {
                    lambdas.add(MethodAnnotation.fromMethodDescriptor(target));
                }
            }
        }
        return lambdas;
    }
}
