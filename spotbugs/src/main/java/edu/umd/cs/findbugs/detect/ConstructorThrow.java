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
package edu.umd.cs.findbugs.detect;

import java.util.*;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.util.BootstrapMethodsUtil;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * This detector can find constructors that throw exception.
 */
public class ConstructorThrow extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;
    private final Map<String, Map<String, Set<String>>> exHandlesToMethodCallsByMethodsMap = new HashMap<>();
    private final Map<String, Set<JavaClass>> thrownExsByMethodMap = new HashMap<>();

    private boolean isFinalClass = false;
    private boolean isFinalFinalizer = false;
    private boolean isFirstPass = true;

    public ConstructorThrow(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    /**
     * Visit a class to find the constructor, then collect all the methods that gets called in it.
     * Also, we are checking for final declaration on the class, or a final finalizer, as if present
     * no finalizer attack can happen.
     */
    @Override
    public void visit(JavaClass obj) {
        resetState();
        if (obj.isFinal()) {
            isFinalClass = true;
            return;
        }
        for (Method m : obj.getMethods()) {
            doVisitMethod(m);
            // Check for final finalizer.
            // Signature of the finalizer is also needed to be checked
            if ("finalize".equals(m.getName()) && "()V".equals(m.getSignature()) && m.isFinal()) {
                isFinalFinalizer = true;
            }
        }
        isFirstPass = false;
    }

    @Override
    public void visit(Method obj) {
        if (isFinalClass || isFinalFinalizer) {
            return;
        }
        if (isConstructor()) {
            // Check if there is a throws keyword for checked exceptions.
            ExceptionTable tbl = obj.getExceptionTable();
            // Check if the number of thrown exceptions is greater than 0
            if (tbl != null && tbl.getNumberOfExceptions() > 0) {
                accumulateBug();
            }
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    /**
     * 1. Check for any throw expression in the constructor.
     * 2. Check for any unchecked exception throw inside constructor,
     * or any of the called methods.
     * If the class is final, we are fine, no finalizer attack can happen.
     * In the first pass the detector shouldn't report, because there could be
     * a final finalizer and a throwing constructor. Reporting in this case
     * would be a false positive as classes with a final finalizer are not
     * vulnerable to the finalizer attack.
     */
    @Override
    public void sawOpcode(int seen) {
        if (isFinalClass || isFinalFinalizer) {
            return;
        }
        if (isFirstPass) {
            collectExeptionsByMethods(seen);
        } else if (isConstructor()) {
            reportConstructorThrow(seen);
        }
    }

    private void reportConstructorThrow(int seen) {
        // if there is a throw in the Constructor which is not handled in a try-catch block, it's a bug
        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                try {
                    JavaClass thrownExClass = item.getJavaClass();
                    Set<String> caughtExes = getSurroundingCaughtExes(getConstantPool());
                    if (isThrownExNotCaught(thrownExClass, caughtExes)) {
                        accumulateBug();
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }
        } else if (isMethodCall(seen)) {
            // checked exceptions are handled when visiting the method
            // if a called method throws a checked exception, and it is not handled, then it must appear in the `throws`
            // here only the explicitly thrown exceptions are examined
            String calledMethodFQN = getCalledMethodFQN(seen);
            Set<JavaClass> unhandledExes = getUndhandledExThrowsInMethod(calledMethodFQN, new HashSet<>());
            if (!unhandledExes.isEmpty()) {
                Set<String> caughtExes = getSurroundingCaughtExes(getConstantPool());
                boolean hasNotCaughtExFromBody = unhandledExes.stream()
                        .anyMatch(ex -> isThrownExNotCaught(ex, caughtExes));

                if (hasNotCaughtExFromBody) {
                    accumulateBug();
                }
            }
        }
    }

    private Set<JavaClass> getUndhandledExThrowsInMethod(String method, Set<String> visitedMethods) {
        Set<JavaClass> unhandledExesInMethod = new HashSet<>();
        if (visitedMethods.contains(method)) {
            return unhandledExesInMethod;
        } else {
            visitedMethods.add(method);
        }

        if (thrownExsByMethodMap.containsKey(method)) {
            unhandledExesInMethod.addAll(thrownExsByMethodMap.get(method));
        }

        if (exHandlesToMethodCallsByMethodsMap.containsKey(method)) {
            Map<String, Set<String>> exHandlesByMethodCalls = exHandlesToMethodCallsByMethodsMap.get(method);
            for (Map.Entry<String, Set<String>> entry : exHandlesByMethodCalls.entrySet()) {
                String calledMethod = entry.getKey();
                Set<JavaClass> unhandledExes = getUndhandledExThrowsInMethod(calledMethod, visitedMethods);
                Set<String> exHandles = entry.getValue();
                Set<JavaClass> remainingUnhandledExes = unhandledExes.stream()
                        .filter(ex -> !isHandled(ex, exHandles))
                        .collect(Collectors.toSet());
                unhandledExesInMethod.addAll(remainingUnhandledExes);
            }
        }
        return unhandledExesInMethod;
    }

    private boolean isHandled(JavaClass thrownEx, Set<String> exHandles) {
        return exHandles.stream().allMatch(handle -> isHandled(thrownEx, handle));
    }

    private static boolean isHandled(JavaClass thrownEx, String caughtEx) {
        try {
            return thrownEx.getClassName().equals(caughtEx)
                    || Arrays.stream(thrownEx.getSuperClasses()).anyMatch(e -> e.getClassName().equals(caughtEx));
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return false;
    }

    private Set<String> getSurroundingCaughtExes(ConstantPool cp) {
        return getSurroundingCaughtExceptionTypes(getPC(), Integer.MAX_VALUE).stream()
                .filter(i -> i != 0)
                .map(caughtExType -> cp.constantToString(cp.getConstant(caughtExType)))
                .collect(Collectors.toSet());
    }

    private static boolean isThrownExNotCaught(JavaClass thrownEx, Set<String> caughtExes) {
        return caughtExes.stream().noneMatch(caughtEx -> isHandled(thrownEx, caughtEx));
    }

    private static String toDotted(String signature) {
        if (signature.startsWith("L") && signature.endsWith(";")) {
            return ClassName.toDottedClassName(signature.substring(1, signature.length() - 1));
        }
        return ClassName.toDottedClassName(signature);
    }

    private void collectExeptionsByMethods(int seen) {
        String containingMethod = getFullyQualifiedMethodName();
        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                try {
                    JavaClass thrownExClass = item.getJavaClass();
                    Set<String> caughtExes = getSurroundingCaughtExes(getConstantPool());
                    if (isThrownExNotCaught(thrownExClass, caughtExes)) {
                        addToThrownExsByMethodMap(containingMethod, thrownExClass);
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }
        } else if (isMethodCall(seen)) {
            String calledMethod = getNameConstantOperand();
            String calledMethodFullName = getCalledMethodFQN(seen);
            // not interested in call of the constructor or recursion
            if (!Const.CONSTRUCTOR_NAME.equals(calledMethod) && !containingMethod.equals(calledMethodFullName)) {
                Set<String> caughtExes = getSurroundingCaughtExes(getConstantPool());
                if (caughtExes.isEmpty()) {
                    // No Exception is handled, then add an empty string to represent this
                    addToExHandlesToMethodCallsByMethodsMap(containingMethod, calledMethodFullName, Collections.singletonList(""));
                } else {
                    addToExHandlesToMethodCallsByMethodsMap(containingMethod, calledMethodFullName, caughtExes);
                }
            }
        }
    }

    private void addToExHandlesToMethodCallsByMethodsMap(String containerMethod, String calledMethod, Collection<String> caughtExes) {
        if (exHandlesToMethodCallsByMethodsMap.containsKey(containerMethod)) {
            Map<String, Set<String>> map = exHandlesToMethodCallsByMethodsMap.get(containerMethod);
            if (!map.containsKey(calledMethod)) {
                map.put(calledMethod, new HashSet<>(caughtExes));
            } else {
                map.get(calledMethod).addAll(caughtExes);
            }
        } else {
            exHandlesToMethodCallsByMethodsMap.put(containerMethod,
                    new HashMap<String, Set<String>>() {
                        {
                            put(calledMethod, new HashSet<>(caughtExes));
                        }
                    });
        }
    }

    private void addToThrownExsByMethodMap(String containingMethod, JavaClass thrownExClass) {
        if (thrownExsByMethodMap.containsKey(containingMethod)) {
            thrownExsByMethodMap.get(containingMethod).add(thrownExClass);
        } else {
            thrownExsByMethodMap.put(containingMethod, new HashSet<>(Collections.singletonList(thrownExClass)));
        }
    }

    private String getCalledMethodFQN(int seen) {
        if (Const.INVOKEDYNAMIC == seen) {
            ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
            for (Attribute attr : getThisClass().getAttributes()) {
                if (attr instanceof BootstrapMethods) {
                    Optional<Method> optMethod = BootstrapMethodsUtil.getMethodFromBootstrap((BootstrapMethods) attr,
                            constDyn.getBootstrapMethodAttrIndex(), getConstantPool(), getThisClass());
                    if (optMethod.isPresent()) {
                        Method method = optMethod.get();
                        return String.format("%s.%s : %s", toDotted(getClassName()), method.getName(),
                                toDotted(method.getSignature()));
                    }
                }
            }
        } else {
            return String.format("%s.%s : %s", getDottedClassConstantOperand(), getNameConstantOperand(),
                    getSigConstantOperand());
        }
        return "";
    }

    private void resetState() {
        isFinalClass = false;
        isFinalFinalizer = false;
        isFirstPass = true;
        exHandlesToMethodCallsByMethodsMap.clear();
        thrownExsByMethodMap.clear();
    }

    private void accumulateBug() {
        BugInstance bug = new BugInstance(this, "CT_CONSTRUCTOR_THROW", NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this, getPC());
        bugAccumulator.accumulateBug(bug, this);
    }

    private boolean isMethodCall(int seen) {
        return seen == Const.INVOKESTATIC
                || seen == Const.INVOKEVIRTUAL
                || seen == Const.INVOKEINTERFACE
                || seen == Const.INVOKESPECIAL
                || seen == Const.INVOKEDYNAMIC;
    }

    private boolean isConstructor() {
        return Const.CONSTRUCTOR_NAME.equals(getMethodName());
    }
}
