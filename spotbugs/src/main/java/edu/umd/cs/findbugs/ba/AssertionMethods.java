/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.SIPUSH;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Mark methodref constant pool entries of methods that are likely to implement
 * assertions. This is useful for pruning likely false paths.
 *
 * @author David Hovemeyer
 */
public class AssertionMethods implements Constants {

    private static final boolean DEBUG = SystemProperties.getBoolean("assertionmethods.debug");

    /**
     * Bitset of methodref constant pool indexes referring to likely assertion
     * methods.
     */
    private final BitSet assertionMethodRefSet;

    private static class UserAssertionMethod {
        private final String className;

        private final String methodName;

        public UserAssertionMethod(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    @edu.umd.cs.findbugs.internalAnnotations.StaticConstant
    private static final List<UserAssertionMethod> userAssertionMethodList = new ArrayList<UserAssertionMethod>();

    static {
        String userProperty = SystemProperties.getProperty("findbugs.assertionmethods");
        if (userProperty != null) {
            StringTokenizer tok = new StringTokenizer(userProperty, ",");
            while (tok.hasMoreTokens()) {
                String fullyQualifiedName = tok.nextToken();
                int lastDot = fullyQualifiedName.lastIndexOf('.');
                if (lastDot < 0) {
                    continue;
                }
                String className = fullyQualifiedName.substring(0, lastDot);
                String methodName = fullyQualifiedName.substring(lastDot + 1);
                userAssertionMethodList.add(new UserAssertionMethod(className, methodName));
            }
        }
    }

    /**
     * Constructor.
     *
     * @param jclass
     *            the JavaClass containing the methodrefs
     */
    public AssertionMethods(JavaClass jclass) {
        this.assertionMethodRefSet = new BitSet();
        init(jclass);
    }

    private void init(JavaClass jclass) {
        ConstantPool cp = jclass.getConstantPool();
        int numConstants = cp.getLength();
        for (int i = 0; i < numConstants; ++i) {
            try {
                Constant c = cp.getConstant(i);
                if (c instanceof ConstantMethodref) {
                    ConstantMethodref cmr = (ConstantMethodref) c;
                    ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex(),
                            CONSTANT_NameAndType);
                    String methodName = ((ConstantUtf8) cp.getConstant(cnat.getNameIndex(), CONSTANT_Utf8)).getBytes();
                    String className = cp.getConstantString(cmr.getClassIndex(), CONSTANT_Class).replace('/', '.');
                    String methodSig = ((ConstantUtf8) cp.getConstant(cnat.getSignatureIndex(), CONSTANT_Utf8)).getBytes();

                    String classNameLC = className.toLowerCase();
                    String methodNameLC = methodName.toLowerCase();

                    boolean voidReturnType = methodSig.endsWith(")V");
                    boolean boolReturnType = methodSig.endsWith(")Z");



                    if (DEBUG) {
                        System.out.print("Is " + className + "." + methodName + " assertion method: " + voidReturnType);
                    }

                    if (isUserAssertionMethod(className, methodName)
                            || className.endsWith("Assert")
                            && methodName.startsWith("is")
                            || (voidReturnType || boolReturnType)
                            && (classNameLC.indexOf("assert") >= 0 || methodNameLC.startsWith("throw")
                            || methodName.startsWith("affirm") || methodName.startsWith("panic")
                            || "logTerminal".equals(methodName) || methodName.startsWith("logAndThrow")
                            || "insist".equals(methodNameLC) || "usage".equals(methodNameLC)
                            || "exit".equals(methodNameLC) || methodNameLC.startsWith("fail")
                            || methodNameLC.startsWith("fatal") || methodNameLC.indexOf("assert") >= 0
                            || methodNameLC.indexOf("legal") >= 0 || methodNameLC.indexOf("error") >= 0
                            || methodNameLC.indexOf("abort") >= 0
                            // || methodNameLC.indexOf("check") >= 0
                            || methodNameLC.indexOf("failed") >= 0) || "addOrThrowException".equals(methodName)) {
                        assertionMethodRefSet.set(i);
                        if (DEBUG) {
                            System.out.println("==> YES");
                        }
                    } else {
                        if (DEBUG) {
                            System.out.println("==> NO");
                        }
                    }
                }
            } catch (ClassFormatException e) {
                // FIXME: should report
            }
        }
    }

    private static boolean isUserAssertionMethod(String className, String methodName) {
        for (UserAssertionMethod uam : userAssertionMethodList) {
            if (className.equals(uam.getClassName()) && methodName.equals(uam.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAssertionHandle(InstructionHandle handle, ConstantPoolGen cpg) {
        Instruction ins = handle.getInstruction();
        if (isAssertionInstruction(ins, cpg)) {
            return true;
        }

        if (ins instanceof SIPUSH) {
            int v = ((SIPUSH) ins).getValue().intValue();
            if (v == 500) {
                Instruction next = handle.getNext().getInstruction();
                if (next instanceof INVOKEINTERFACE) {
                    INVOKEINTERFACE iInterface = (INVOKEINTERFACE) next;
                    String className = iInterface.getClassName(cpg);
                    String fieldName = iInterface.getMethodName(cpg);
                    if ("javax.servlet.http.HttpServletResponse".equals(className) && "setStatus".equals(fieldName)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    /**
     * Does the given instruction refer to a likely assertion method?
     *
     * @param ins
     *            the instruction
     * @return true if the instruction likely refers to an assertion, false if
     *         not
     */

    public boolean isAssertionInstruction(Instruction ins, ConstantPoolGen cpg) {

        if (ins instanceof InvokeInstruction) {
            return isAssertionCall((InvokeInstruction) ins);
        }
        if (ins instanceof GETSTATIC) {
            GETSTATIC getStatic = (GETSTATIC) ins;
            String className = getStatic.getClassName(cpg);
            String fieldName = getStatic.getFieldName(cpg);
            if ("java.util.logging.Level".equals(className) && "SEVERE".equals(fieldName)) {
                return true;
            }
            if ("org.apache.log4j.Level".equals(className) && ("ERROR".equals(fieldName) || "FATAL".equals(fieldName))) {
                return true;
            }
            return false;

        }
        return false;
    }

    public boolean isAssertionCall(InvokeInstruction inv) {

        boolean isAssertionMethod = assertionMethodRefSet.get(inv.getIndex());

        return isAssertionMethod;
    }
}

