package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MethodGen;

/**
 * Utility class with methods to identify multi threaded code
 */
public class MultiThreadedCodeIdentifierUtils {

    private static final String RUNNABLE_SIGNATURE = "java.lang.Runnable";
    private static final String ATOMIC_PACKAGE = "java.util.concurrent.atomic";

    public static boolean isPartOfMultiThreadedCode(ClassContext classContext) {
        JavaClass classToCheck = classContext.getJavaClass();
        if (implementsRunnable(classToCheck)) {
            return true;
        }

        if (Stream.of(classToCheck.getFields()).anyMatch(MultiThreadedCodeIdentifierUtils::isFieldMultiThreaded)) {
            return true;
        }

        return Stream.of(classToCheck.getMethods()).anyMatch(method -> isMethodMultiThreaded(method, classContext));
    }

    public static boolean implementsRunnable(JavaClass javaClass) {
        try {
            return Stream.of(javaClass.getAllInterfaces())
                    .anyMatch(ifc -> ifc.getClassName().equals(RUNNABLE_SIGNATURE));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isMethodMultiThreaded(Method method, ClassContext classContext) {
        if (method.isSynchronized()) {
            return true;
        }

        LocalVariableTable lvt = method.getLocalVariableTable();
        if (lvt != null && Stream.of(lvt.getLocalVariableTable())
                .anyMatch(lv -> signatureIsFromAtomicPackage(lv.getSignature()))) {
            return true;
        }

        Optional<MethodGen> maybeMethodGen = Optional.ofNullable(classContext.getMethodGen(method));
        if (!maybeMethodGen.isPresent()) {
            return false;
        }

        MethodGen methodGen = maybeMethodGen.get();
        return hasMultiThreadedInstruction(methodGen);
    }

    public static boolean hasMultiThreadedInstruction(MethodGen methodGen) {
        ConstantPoolGen cpg = methodGen.getConstantPool();
        InstructionHandle handle = methodGen.getInstructionList().getStart();
        while (handle != null) {
            Instruction ins = handle.getInstruction();
            if (ins instanceof MONITORENTER) {
                return true;
            } else if (ins instanceof INVOKEVIRTUAL) {
                String methodName = ((INVOKEVIRTUAL) ins).getMethodName(cpg);
                if (isConcurrentLockInterfaceCall(methodName)) {
                    return true;
                }
            }
            handle = handle.getNext();
        }

        return false;
    }

    public static boolean isConcurrentLockInterfaceCall(String methodName) {
        return "lock".equals(methodName)
                || "unlock".equals(methodName)
                || "tryLock".equals(methodName)
                || "lockInterruptibly".equals(methodName);
    }

    public static boolean isFieldMultiThreaded(Field field) {
        return field.isVolatile() || signatureIsFromAtomicPackage(field.getSignature());
    }

    public static boolean isFieldMultiThreaded(XField field) {
        return field.isVolatile() || signatureIsFromAtomicPackage(field.getSignature());
    }

    public static boolean signatureIsFromAtomicPackage(String signature) {
        return Utility.signatureToString(signature).startsWith(ATOMIC_PACKAGE);
    }
}
