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

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MethodGen;

/**
 * Utility class with methods to identify multithreaded code
 */
public class MultiThreadedCodeIdentifierUtils {

    private MultiThreadedCodeIdentifierUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String RUNNABLE_SIGNATURE = "java.lang.Runnable";
    private static final String ATOMIC_PACKAGE = "java/util/concurrent/atomic";

    public static boolean isPartOfMultiThreadedCode(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        if (Subtypes2.instanceOf(javaClass, RUNNABLE_SIGNATURE) ||
                Stream.of(javaClass.getFields()).anyMatch(MultiThreadedCodeIdentifierUtils::isFieldMultiThreaded)) {
            return true;
        }

        return Stream.of(javaClass.getMethods()).anyMatch(method -> isMethodMultiThreaded(method, classContext));
    }

    public static boolean isMethodMultiThreaded(Method method, ClassContext classContext) {
        if (method.isSynchronized()) {
            return true;
        }

        LocalVariableTable lvt = method.getLocalVariableTable();
        if (lvt != null && Stream.of(lvt.getLocalVariableTable()).anyMatch(lv -> isFromAtomicPackage(lv.getSignature()))) {
            return true;
        }

        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen != null) {
            return hasMultiThreadedInstruction(methodGen);
        } else {
            return false;
        }
    }

    private static boolean hasMultiThreadedInstruction(MethodGen methodGen) {
        ConstantPoolGen cpg = methodGen.getConstantPool();

        InstructionHandle handle = methodGen.getInstructionList().getStart();
        while (handle != null) {
            Instruction ins = handle.getInstruction();
            if (ins instanceof MONITORENTER) {
                return true;
            } else if (ins instanceof INVOKEVIRTUAL) {
                INVOKEVIRTUAL invokevirtual = ((INVOKEVIRTUAL) ins);
                String className = invokevirtual.getClassName(cpg);
                String methodName = invokevirtual.getMethodName(cpg);
                if (isConcurrentLockInterfaceCall(className, methodName)) {
                    return true;
                }
            }
            handle = handle.getNext();
        }

        return false;
    }

    private static boolean isConcurrentLockInterfaceCall(String className, String methodName) {
        return Subtypes2.instanceOf(className, "java.util.concurrent.locks.Lock")
                && ("lock".equals(methodName)
                        || "unlock".equals(methodName)
                        || "tryLock".equals(methodName)
                        || "lockInterruptibly".equals(methodName));
    }

    private static boolean isFieldMultiThreaded(Field field) {
        return field.isVolatile() || isFromAtomicPackage(field.getSignature());
    }

    public static boolean isFieldMultiThreaded(XField field) {
        return field.isVolatile() || isFromAtomicPackage(field.getSignature());
    }

    public static boolean isFromAtomicPackage(String signature) {
        return signature.contains(ATOMIC_PACKAGE);
    }
}
