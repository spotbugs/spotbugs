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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;

import java.util.Collection;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MethodGen;

/**
 * Utility class with methods to identify multithreaded code
 */
public class MultiThreadedCodeIdentifierUtils {

    private MultiThreadedCodeIdentifierUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String JAVA_LANG_RUNNABLE = "java.lang.Runnable";
    private static final String ATOMIC_PACKAGE = "java/util/concurrent/atomic";

    public static boolean isPartOfMultiThreadedCode(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        if (Subtypes2.instanceOf(javaClass, JAVA_LANG_RUNNABLE) ||
                Stream.of(javaClass.getFields()).anyMatch(MultiThreadedCodeIdentifierUtils::isFieldIndicatingMultiThreadedContainer)) {
            return true;
        }

        return Stream.of(javaClass.getMethods()).anyMatch(method -> isMethodMultiThreaded(method, classContext));
    }

    public static boolean isMethodMultiThreaded(Method method, ClassContext classContext) {
        // TODO: probably should check that as well to avoid false positives?
        // if (method.isSynthetic()) {
        // return false;
        // }

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
            } else if (ins instanceof InvokeInstruction && !(ins instanceof INVOKEDYNAMIC)) {
                InvokeInstruction iins = (InvokeInstruction) ins;
                String className = iins.getClassName(cpg);
                String methodName = iins.getMethodName(cpg);
                if (isConcurrentLockInterfaceCall(className, methodName) || CollectionAnalysis.isSynchronizedCollection(className, methodName)) {
                    return true;
                }
            }
            handle = handle.getNext();
        }

        return false;
    }

    private static boolean isConcurrentLockInterfaceCall(@DottedClassName String className, String methodName) {
        return isInstanceOfLock(className)
                && ("lock".equals(methodName)
                        || "unlock".equals(methodName)
                        || "tryLock".equals(methodName)
                        || "lockInterruptibly".equals(methodName));
    }

    private static boolean isInstanceOfLock(@DottedClassName String className) {
        return className != null && Subtypes2.instanceOf(className, "java.util.concurrent.locks.Lock");
    }

    private static boolean isFieldIndicatingMultiThreadedContainer(Field field) {
        if (field.isSynthetic()) {
            return false;
        }
        return field.isVolatile() || isFromAtomicPackage(field.getSignature())
                || isInstanceOfLock(ClassName.fromFieldSignatureToDottedClassName(field.getSignature()));
    }

    private static boolean isFromAtomicPackage(String signature) {
        return signature.contains(ATOMIC_PACKAGE);
    }

    public static boolean isLocked(Method currentMethod, CFG currentCFG, LockDataflow currentLockDataFlow, int pc) {
        try {
            if (currentMethod != null && currentLockDataFlow != null && currentCFG != null) {
                Collection<Location> tLocations = currentCFG.getLocationsContainingInstructionWithOffset(pc);
                for (Location tLoc : tLocations) {
                    LockSet lockSet = currentLockDataFlow.getFactAtLocation(tLoc);
                    if (lockSet.getNumLockedObjects() > 0) {
                        // within a synchronized block
                        return true;
                    }
                }
            }
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError(String.format("Synchronization check caught an error when analyzing %s method.", currentMethod.getName()), e);
        }
        return false;
    }
}
