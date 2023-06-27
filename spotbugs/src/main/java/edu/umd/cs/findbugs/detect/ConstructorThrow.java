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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.CodeException;
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
            // Signature of the finalizer is also needed to be checked
            if ("finalize".equals(m.getName()) && "()V".equals(m.getSignature())) {
                // Check for final finalizer.
                if (m.isFinal()) {
                    isFinalFinalizer = true;
                }
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
     *    or any of the called methods.
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
        } else {
            if (isConstructor()) {
                if (seen == Const.ATHROW) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    if (item != null) {
                        try {
                            JavaClass thrownExClass = item.getJavaClass();
                            CodeException tryBlock = getSurroundingTryBlock(getPC());
                            if (tryBlock != null) {
                                if (isThrownExNotCaught(thrownExClass, tryBlock, getConstantPool())) {
                                    accumulateBug();
                                }
                            } else {
                                accumulateBug();
                            }
                        } catch (ClassNotFoundException e) {
                            AnalysisContext.reportMissingClass(e);
                        }
                    }
                } else if (isMethodCall(seen)) {
                    XMethod calledXMethod = getXMethodOperand();
                    String calledMethodName = getCalledMethodFQN();
                    if (!Const.CONSTRUCTOR_NAME.equals(calledXMethod.getName())) {
                        String[] thrownExes = calledXMethod.getThrownExceptions();
                        Set<JavaClass> unhandledExes = getUndhandledExesInMethod(calledMethodName);
                        if (thrownExes != null || !unhandledExes.isEmpty()) {
                            CodeException tryBlock = getSurroundingTryBlock(getPC());
                            if (tryBlock != null) {
                                boolean hasNotCaughtExFromThrows = thrownExes == null ? false
                                        : Arrays.stream(thrownExes)
                                                .map(ConstructorThrow::toDotted)
                                                .anyMatch(ex -> isThrownExNotCaught(ex, tryBlock, getConstantPool()));

                                boolean hasNotCaughtExFromBody = unhandledExes.stream()
                                        .anyMatch(ex -> isThrownExNotCaught(ex, tryBlock, getConstantPool()));

                                if (hasNotCaughtExFromThrows || hasNotCaughtExFromBody) {
                                    accumulateBug();
                                }
                            } else {
                                accumulateBug();
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<JavaClass> getUndhandledExesInMethod(String method) {
        Set<JavaClass> unhandledExesInMethod = new HashSet<>();
        if (thrownExsByMethodMap.containsKey(method)) {
            unhandledExesInMethod.addAll(thrownExsByMethodMap.get(method));
        }

        if (exHandlesToMethodCallsByMethodsMap.containsKey(method)) {
            Map<String, Set<String>> exHandlesByMethodCalls = exHandlesToMethodCallsByMethodsMap.get(method);
            for (Map.Entry<String, Set<String>> entry : exHandlesByMethodCalls.entrySet()) {
                String calledMethod = entry.getKey();
                Set<JavaClass> unhandledExes = getUndhandledExesInMethod(calledMethod);
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

    private static boolean isThrownExNotCaught(JavaClass thrownEx, CodeException tryBlock, ConstantPool constantPool) {
        int caughtExType = tryBlock.getCatchType();
        if (caughtExType != 0) {
            String caughtExName = constantPool.constantToString(constantPool.getConstant(caughtExType));
            return !isHandled(thrownEx, caughtExName);
        }
        return true;
    }

    private static boolean isThrownExNotCaught(String thrownEx, CodeException tryBlock, ConstantPool constantPool) {
        int caughtExType = tryBlock.getCatchType();
        if (caughtExType != 0) {
            String caughtExName = constantPool.constantToString(constantPool.getConstant(caughtExType));
            // TODO any caughtEx is parent of thrownEx
            return !thrownEx.equals(caughtExName);
        }
        return false;
    }

    private static String toDotted(String signature) {
        if (signature.startsWith("L") && signature.endsWith(";")) {
            return signature.substring(1, signature.length() - 1).replace('/', '.');
        }
        return signature.replace('/', '.');
    }

    private void collectExeptionsByMethods(int seen) {
        String surroundingMethod = getFullyQualifiedMethodName();
        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                try {
                    JavaClass thrownExClass = item.getJavaClass();
                    CodeException tryBlock = getSurroundingTryBlock(getPC());
                    if (tryBlock != null) {
                        if (isThrownExNotCaught(thrownExClass, tryBlock, getConstantPool())) {
                            if (thrownExsByMethodMap.containsKey(surroundingMethod)) {
                                thrownExsByMethodMap.get(surroundingMethod).add(thrownExClass);
                            } else {
                                thrownExsByMethodMap.put(surroundingMethod, new HashSet<JavaClass>() {
                                    {
                                        add(thrownExClass);
                                    }
                                });
                            }
                        }
                    } else {
                        if (thrownExsByMethodMap.containsKey(surroundingMethod)) {
                            thrownExsByMethodMap.get(surroundingMethod).add(thrownExClass);
                        } else {
                            thrownExsByMethodMap.put(surroundingMethod, new HashSet<JavaClass>() {
                                {
                                    add(thrownExClass);
                                }
                            });
                        }
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }
        } else if (isMethodCall(seen)) {
            String calledMethod = getNameConstantOperand();
            String calledMethodFullName = getCalledMethodFQN();
            // not interested in call of the constructor or recursion
            if (!Const.CONSTRUCTOR_NAME.equals(calledMethod) && !surroundingMethod.equals(calledMethodFullName)) {
                CodeException tryBlock = getSurroundingTryBlock(getPC());
                if (tryBlock != null) {
                    int caughtExType = tryBlock.getCatchType();
                    if (caughtExType != 0) {
                        ConstantPool cp = getConstantPool();
                        String caughtExName = cp.constantToString(cp.getConstant(caughtExType));
                        if (exHandlesToMethodCallsByMethodsMap.containsKey(surroundingMethod)) {
                            Map<String, Set<String>> map = exHandlesToMethodCallsByMethodsMap.get(surroundingMethod);
                            if (!map.containsKey(calledMethodFullName)) {
                                map.put(calledMethodFullName, new HashSet<String>() {
                                    {
                                        add(caughtExName);
                                    }
                                });
                            } else {
                                map.get(calledMethodFullName).add(caughtExName);
                            }
                        } else {
                            exHandlesToMethodCallsByMethodsMap.put(surroundingMethod,
                                    new HashMap<String, Set<String>>() {
                                        {
                                            put(calledMethodFullName, new HashSet<String>() {
                                                {
                                                    add(caughtExName);
                                                }
                                            });
                                        }
                                    });
                        }
                    }
                } else {
                    // No Exception is handled, then add an empty string to represent this
                    if (exHandlesToMethodCallsByMethodsMap.containsKey(surroundingMethod)) {
                        Map<String, Set<String>> map = exHandlesToMethodCallsByMethodsMap.get(surroundingMethod);
                        if (!map.containsKey(calledMethodFullName)) {
                            map.put(calledMethodFullName, new HashSet<String>() {
                                {
                                    add("");
                                }
                            });
                        } else {
                            map.get(calledMethodFullName).add("");
                        }
                    } else {
                        exHandlesToMethodCallsByMethodsMap.put(surroundingMethod,
                                new HashMap<String, Set<String>>() {
                                    {
                                        put(calledMethodFullName, new HashSet<String>() {
                                            {
                                                add("");
                                            }
                                        });
                                    }
                                });
                    }
                }
            }
        }
    }

    private String getCalledMethodFQN() {
        return String.format("%s.%s : %s", getDottedClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand());
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
        return seen == Const.INVOKESTATIC || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL;
    }

    private boolean isConstructor() {
        return Const.CONSTRUCTOR_NAME.equals(getMethodName());
    }
}
