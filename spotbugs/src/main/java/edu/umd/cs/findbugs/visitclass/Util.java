/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.visitclass;

import javax.annotation.CheckForNull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;


/**
 * @author pugh
 */
public class Util {
    /**
     * Determine the outer class of obj.
     *
     * @param obj
     * @return JavaClass for outer class, or null if obj is not an outer class
     * @throws ClassNotFoundException
     */

    @CheckForNull
    public static JavaClass getOuterClass(JavaClass obj) throws ClassNotFoundException {
        for (Attribute a : obj.getAttributes()) {
            if (a instanceof InnerClasses) {
                for (InnerClass ic : ((InnerClasses) a).getInnerClasses()) {
                    if (obj.getClassNameIndex() == ic.getInnerClassIndex()) {
                        // System.out.println("Outer class is " +
                        // ic.getOuterClassIndex());
                        ConstantClass oc = (ConstantClass) obj.getConstantPool().getConstant(ic.getOuterClassIndex());
                        String ocName = oc.getBytes(obj.getConstantPool());
                        return Repository.lookupClass(ocName);
                    }
                }
            }
        }
        return null;
    }

    public static int getSizeOfSurroundingTryBlock(@CheckForNull Method method, Class<? extends Throwable> exceptionClass, int pc) {
        if (method == null) {
            return Integer.MAX_VALUE;
        }

        return getSizeOfSurroundingTryBlock(method, ClassName.toSlashedClassName(exceptionClass), pc);
    }

    public static int getSizeOfSurroundingTryBlock(@CheckForNull Method method, @CheckForNull String vmNameOfExceptionClass, int pc) {
        if (method == null) {
            return Integer.MAX_VALUE;
        }
        return getSizeOfSurroundingTryBlock(method.getConstantPool(), method.getCode(), vmNameOfExceptionClass, pc);
    }

    public static @CheckForNull CodeException getSurroundingTryBlock(ConstantPool constantPool, Code code,
            @CheckForNull String vmNameOfExceptionClass, int pc) {
        int size = Integer.MAX_VALUE;
        if (code.getExceptionTable() == null) {
            return null;
        }
        CodeException result = null;
        for (CodeException catchBlock : code.getExceptionTable()) {
            if (vmNameOfExceptionClass != null) {
                Constant catchType = constantPool.getConstant(catchBlock.getCatchType());
                if (catchType == null && !vmNameOfExceptionClass.isEmpty() || catchType instanceof ConstantClass
                        && !((ConstantClass) catchType).getBytes(constantPool).equals(vmNameOfExceptionClass)) {
                    continue;
                }
            }
            int startPC = catchBlock.getStartPC();
            int endPC = catchBlock.getEndPC();
            if (pc >= startPC && pc <= endPC) {
                int thisSize = endPC - startPC;
                if (size > thisSize) {
                    size = thisSize;
                    result = catchBlock;
                }
            }
        }
        return result;
    }

    public static int getSizeOfSurroundingTryBlock(ConstantPool constantPool, Code code,
            @CheckForNull @SlashedClassName String vmNameOfExceptionClass, int pc) {
        int size = Integer.MAX_VALUE;
        int tightStartPC = 0;
        int tightEndPC = Integer.MAX_VALUE;
        if (code.getExceptionTable() == null) {
            return size;
        }
        for (CodeException catchBlock : code.getExceptionTable()) {
            if (vmNameOfExceptionClass != null) {
                if (catchBlock.getCatchType() == 0) {
                    continue;
                } else {
                    Constant catchType = constantPool.getConstant(catchBlock.getCatchType());
                    if (catchType == null && !vmNameOfExceptionClass.isEmpty() || catchType instanceof ConstantClass
                            && !((ConstantClass) catchType).getBytes(constantPool).equals(vmNameOfExceptionClass)) {
                        continue;
                    }
                }
            }
            int startPC = catchBlock.getStartPC();
            int endPC = catchBlock.getEndPC();
            if (pc >= startPC && pc <= endPC) {
                int thisSize = endPC - startPC;
                if (size > thisSize) {
                    size = thisSize;
                    tightStartPC = startPC;
                    tightEndPC = endPC;
                }
            }
        }
        if (size == Integer.MAX_VALUE) {
            return size;
        }

        // try to guestimate number of lines that correspond
        size = (size + 7) / 8;
        LineNumberTable lineNumberTable = code.getLineNumberTable();
        if (lineNumberTable == null) {
            return size;
        }

        int count = 0;
        for (LineNumber line : lineNumberTable.getLineNumberTable()) {
            if (line.getStartPC() > tightEndPC) {
                break;
            }
            if (line.getStartPC() >= tightStartPC) {
                count++;
            }
        }
        return count;

    }

}
