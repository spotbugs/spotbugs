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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Utility methods for working with bootstrap methods
 *
 * @author Ádám Balogh
 */
public class BootstrapMethodsUtil {

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
            if (!metOpt.isPresent()) {
                continue;
            }
            return metOpt;
        }
        return Optional.empty();
    }

    /**
     * Returns application methods referenced by method handles in a bootstrap method.
     */
    public static List<MethodDescriptor> getCalledMethodsFromBootstrap(BootstrapMethods bms, int index, ConstantPool cp) {
        List<MethodDescriptor> result = new ArrayList<>();
        BootstrapMethod bm = bms.getBootstrapMethods()[index];
        for (int arg : bm.getBootstrapArguments()) {
            MethodDescriptor called = getMethodDescriptorFromBootstrapArgument(bms, cp, arg);
            if (called != null) {
                result.add(called);
            }
        }
        return result;
    }

    /**
     * Returns all application methods referenced by method handles in a class bootstrap table.
     */
    public static List<MethodDescriptor> getCalledMethodsFromBootstrapMethods(BootstrapMethods bms, ConstantPool cp) {
        List<MethodDescriptor> result = new ArrayList<>();
        for (int i = 0; i < bms.getBootstrapMethods().length; i++) {
            result.addAll(getCalledMethodsFromBootstrap(bms, i, cp));
        }
        return result;
    }

    private static MethodDescriptor getMethodDescriptorFromBootstrapArgument(BootstrapMethods bms, ConstantPool cp,
            int arg) {
        Constant c = bms.getConstantPool().getConstant(arg);
        if (!(c instanceof ConstantMethodHandle)) {
            return null;
        }
        ConstantMethodHandle cmh = (ConstantMethodHandle) c;
        c = cp.getConstant(cmh.getReferenceIndex());
        if (!(c instanceof ConstantMethodref) && !(c instanceof ConstantInterfaceMethodref)) {
            return null;
        }
        ConstantCP ccp = (ConstantCP) c;
        ConstantClass cc = (ConstantClass) cp.getConstant(ccp.getClassIndex());
        String className = cc.getBytes(cp);
        ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(ccp.getNameAndTypeIndex());
        boolean isStatic = cmh.getReferenceKind() == org.apache.bcel.Const.REF_invokeStatic;
        return DescriptorFactory.instance().getMethodDescriptor(className, cnat.getName(cp), cnat.getSignature(cp), isStatic);
    }
}
