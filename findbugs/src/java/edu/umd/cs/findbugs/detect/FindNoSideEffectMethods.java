/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author Tagir Valeev
 */
public class FindNoSideEffectMethods extends OpcodeStackDetector implements NonReportingDetector {
    private static final MethodDescriptor GET_CLASS = new MethodDescriptor("java/lang/Object", "getClass", "()Ljava/lang/Class;");
    private static final MethodDescriptor ARRAY_COPY = new MethodDescriptor("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", true);
    private static final MethodDescriptor HASH_CODE = new MethodDescriptor("java/lang/Object", "hashCode", "()I");
    private static final MethodDescriptor CLASS_GET_NAME = new MethodDescriptor("java/lang/Class", "getName", "()Ljava/lang/String;");
    // Stub method to generalize array store
    private static final MethodDescriptor ARRAY_STORE_STUB_METHOD = new MethodDescriptor("java/lang/Array", "set", "(ILjava/lang/Object;)V");
    // Stub method to generalize field store
    private static final MethodDescriptor FIELD_STORE_STUB_METHOD = new MethodDescriptor("java/lang/Object", "putField", "(Ljava/lang/Object;)V");

    // Fictional method call targets
    private static final FieldDescriptor TARGET_THIS = new FieldDescriptor("java/lang/Stub", "this", "V", false);
    private static final FieldDescriptor TARGET_NEW = new FieldDescriptor("java/lang/Stub", "new", "V", false);
    private static final FieldDescriptor TARGET_OTHER = new FieldDescriptor("java/lang/Stub", "other", "V", false);

    private static final Set<String> NUMBER_CLASSES = new HashSet<>(Arrays.asList("java/lang/Integer", "java/lang/Long",
            "java/lang/Double", "java/lang/Float", "java/lang/Byte", "java/lang/Short", "java/math/BigInteger",
            "java/math/BigDecimal"));

    private static final Set<String> ALLOWED_EXCEPTIONS = new HashSet<>(Arrays.asList("java.lang.InternalError",
            "java.lang.ArrayIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException",
            "java.lang.IndexOutOfBoundsException"));

    private static final Set<String> NO_SIDE_EFFECT_COLLECTION_METHODS = new HashSet<>(Arrays.asList("contains", "containsKey",
            "containsValue", "get", "indexOf", "lastIndexOf", "iterator", "listIterator", "isEmpty", "size", "getOrDefault",
            "subList", "keys", "elements", "keySet", "entrySet", "values", "stream", "firstKey", "lastKey", "headMap", "tailMap",
            "subMap", "peek", "mappingCount"));

    private static final Set<String> OBJECT_ONLY_CLASSES = new HashSet<>(Arrays.asList("java/lang/StringBuffer",
            "java/lang/StringBuilder", "java/util/regex/Matcher", "java/io/ByteArrayOutputStream",
            "java/util/concurrent/atomic/AtomicBoolean", "java/util/concurrent/atomic/AtomicInteger",
            "java/util/concurrent/atomic/AtomicLong", "java/awt/Point"));

    // Usual implementation of stub methods which are expected to be more complex in derived classes
    private static final byte[][] STUB_METHODS = new byte[][] {
        {(byte) RETURN},
        {ICONST_0, (byte) IRETURN},
        {ICONST_1, (byte) IRETURN},
        {ICONST_M1, (byte) IRETURN},
        {LCONST_0, (byte) LRETURN},
        {FCONST_0, (byte) FRETURN},
        {DCONST_0, (byte) DRETURN},
        {ACONST_NULL, (byte) ARETURN},
        {ALOAD_0, (byte) ARETURN},
        {ALOAD_1, (byte) ARETURN},
    };

    /**
     * Known methods which change only this object
     */
    private static final Set<MethodDescriptor> OBJECT_ONLY_METHODS = new HashSet<>(Arrays.asList(
            ARRAY_STORE_STUB_METHOD, FIELD_STORE_STUB_METHOD,
            new MethodDescriptor("java/util/Iterator", "next", "()Ljava/lang/Object;"),
            new MethodDescriptor("java/util/Enumeration", "nextElement", "()Ljava/lang/Object;"),
            new MethodDescriptor("java/lang/Throwable", "fillInStackTrace", "()Ljava/lang/Throwable;")
            ));

    /**
     * Known methods which have no side-effect
     */
    private static final Set<MethodDescriptor> NO_SIDE_EFFECT_METHODS = new HashSet<>(Arrays.asList(
            GET_CLASS, CLASS_GET_NAME, HASH_CODE,
            new MethodDescriptor("java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;"),
            new MethodDescriptor("java/lang/Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;"),
            new MethodDescriptor("java/lang/Class", "getSimpleName", "()Ljava/lang/String;"),
            new MethodDescriptor("java/lang/Class", "getMethods", "()[Ljava/lang/reflect/Method;"),
            new MethodDescriptor("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;"),
            new MethodDescriptor("java/lang/Runtime", "availableProcessors", "()I"),
            new MethodDescriptor("java/lang/Runtime", "maxMemory", "()J"),
            new MethodDescriptor("java/lang/Runtime", "totalMemory", "()J"),
            new MethodDescriptor("java/lang/Iterable", "iterator", "()Ljava/util/Iterator;"),
            new MethodDescriptor("java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I"),
            new MethodDescriptor("java/util/Arrays", "deepEquals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", true),
            new MethodDescriptor("java/util/Enumeration", "hasMoreElements", "()Z"),
            new MethodDescriptor("java/util/Iterator", "hasNext", "()Z"),
            new MethodDescriptor("java/util/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I"),
            new MethodDescriptor("java/util/logging/LogManager", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", true),
            new MethodDescriptor("org/apache/log4j/LogManager", "getLogger", "(Ljava/lang/String;)Lorg/apache/log4j/Logger;", true)
            ));

    private static final Set<MethodDescriptor> NEW_OBJECT_RETURNING_METHODS = new HashSet<>(Arrays.asList(
            new MethodDescriptor("java/util/Vector", "elements", "()Ljava/util/Enumeration;"),
            new MethodDescriptor("java/util/Hashtable", "elements", "()Ljava/util/Enumeration;"),
            new MethodDescriptor("java/util/Hashtable", "keys", "()Ljava/util/Enumeration;"),
            new MethodDescriptor("java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;")
            ));

    private static enum SideEffectStatus {
        SIDE_EFFECT, UNSURE_OBJECT_ONLY, OBJECT_ONLY, UNSURE, NO_SIDE_EFFECT;

        boolean unsure() {
            return this == UNSURE || this == UNSURE_OBJECT_ONLY;
        }

        SideEffectStatus toObjectOnly() {
            switch(this) {
            case UNSURE:
                return UNSURE_OBJECT_ONLY;
            case NO_SIDE_EFFECT:
                return OBJECT_ONLY;
            default:
                return this;
            }
        }

        SideEffectStatus toUnsure() {
            switch(this) {
            case OBJECT_ONLY:
                return UNSURE_OBJECT_ONLY;
            case NO_SIDE_EFFECT:
                return UNSURE;
            default:
                return this;
            }
        }

        SideEffectStatus toSure() {
            switch(this) {
            case UNSURE_OBJECT_ONLY:
                return OBJECT_ONLY;
            case UNSURE:
                return NO_SIDE_EFFECT;
            default:
                return this;
            }
        }
    }

    private static class MethodCall {
        private final MethodDescriptor method;
        private final FieldDescriptor target;

        public MethodCall(MethodDescriptor method, FieldDescriptor target) {
            this.method = method;
            this.target = target;
        }

        public MethodDescriptor getMethod() {
            return method;
        }

        public FieldDescriptor getTarget() {
            return target;
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MethodCall other = (MethodCall) obj;
            if (!method.equals(other.method)) {
                return false;
            }
            if (!target.equals(other.target)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Public status of the method in NSE database
     * TODO: implement CHECK
     */
    public static enum MethodSideEffectStatus {
        NSE, // Non-void method has no side effect
        NSE_EX, // No side effect method which result value might be ignored for some reason
        CHECK, // (unimplemented yet) No side effect method which just checks the arguments, throws exceptions and returns one of arguments (or void) like assert or precondition
        USELESS, // Void method which seems to be useless
        SE_CLINIT, // Method has no side effect, but it's a constructor or static method of the class having side effect
        OBJ, // Non-static method which changes only its object
        SE // Method has side effect or side-effect status for the method is unknown
    }

    public static class NoSideEffectMethodsDatabase {
        private final Map<MethodDescriptor, MethodSideEffectStatus> map = new HashMap<>();

        void add(MethodDescriptor m, MethodSideEffectStatus s) {
            map.put(m, s);
        }

        public @Nonnull MethodSideEffectStatus status(MethodDescriptor m) {
            MethodSideEffectStatus s = map.get(m);
            return s == null ? MethodSideEffectStatus.SE : s;
        }

        /**
         * @param m method to check
         * @param statuses allowed statuses
         * @return true if method status is one of the statuses
         */
        public boolean is(MethodDescriptor m, MethodSideEffectStatus... statuses) {
            MethodSideEffectStatus s = status(m);
            for(MethodSideEffectStatus status : statuses) {
                if(s == status) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasNoSideEffect(MethodDescriptor m) {
            return status(m) == MethodSideEffectStatus.NSE;
        }

        public boolean useless(MethodDescriptor m) {
            return status(m) == MethodSideEffectStatus.USELESS;
        }

        public boolean excluded(MethodDescriptor m) {
            return is(m, MethodSideEffectStatus.NSE_EX, MethodSideEffectStatus.SE_CLINIT);
        }
    }

    static class EarlyExitException extends RuntimeException {
    }

    private final Map<MethodDescriptor, SideEffectStatus> statusMap = new HashMap<>();
    private final Map<MethodDescriptor, List<MethodCall>> callGraph = new HashMap<>();
    private final Set<MethodDescriptor> getStaticMethods = new HashSet<>();
    private final Set<MethodDescriptor> uselessVoidCandidates = new HashSet<>();

    private SideEffectStatus status;
    private ArrayList<MethodCall> calledMethods;
    private Set<ClassDescriptor> subtypes;
    private Set<Integer> finallyTargets;
    private Set<Integer> finallyExceptionRegisters;

    private boolean constructor;
    private boolean uselessVoidCandidate;
    private boolean classInit;

    private Set<FieldDescriptor> allowedFields;
    private Set<MethodDescriptor> fieldsModifyingMethods;

    private final NoSideEffectMethodsDatabase noSideEffectMethods = new NoSideEffectMethodsDatabase();

    public FindNoSideEffectMethods(BugReporter bugReporter) {
        Global.getAnalysisCache().eagerlyPutDatabase(NoSideEffectMethodsDatabase.class, noSideEffectMethods);
    }

    @Override
    public void visit(JavaClass obj) {
        super.visit(obj);
        allowedFields = new HashSet<>();
        fieldsModifyingMethods = new HashSet<>();
        subtypes = null;
        if (!obj.isFinal() && !obj.isEnum()) {
            try {
                Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
                subtypes = new HashSet<>(subtypes2.getSubtypes(getClassDescriptor()));
                subtypes.remove(getClassDescriptor());
            } catch (ClassNotFoundException e) {
            }
        }
    }

    @Override
    public void visit(Method method) {
        constructor = method.getName().equals("<init>");
        classInit = method.getName().equals("<clinit>");
        calledMethods = new ArrayList<>();
        status = SideEffectStatus.NO_SIDE_EFFECT;
        if (hasNoSideEffect(getMethodDescriptor())) {
            handleStatus();
            return;
        }
        if(isObjectOnlyMethod(getMethodDescriptor())) {
            status = SideEffectStatus.OBJECT_ONLY;
        }
        if (method.isNative() || changedArg(getMethodDescriptor()) != -1) {
            status = SideEffectStatus.SIDE_EFFECT;
            handleStatus();
            return;
        }
        boolean sawImplementation = false;
        if (classInit) {
            superClinitCall();
        }
        if (!method.isStatic() && !method.isPrivate() && !method.isFinal() && !constructor && subtypes != null) {
            for (ClassDescriptor subtype : subtypes) {
                try {
                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, subtype);
                    XMethod matchingMethod = xClass.findMatchingMethod(getMethodDescriptor());
                    if (matchingMethod != null) {
                        sawImplementation = true;
                        sawCall(new MethodCall(matchingMethod.getMethodDescriptor(), TARGET_THIS), false);
                    }
                } catch (CheckedAnalysisException e) {
                }
            }
        }
        if (method.isAbstract() || method.isInterface()) {
            if (!sawImplementation
                    || getClassName().endsWith("Visitor") || getClassName().endsWith("Listener")
                    || getClassName().startsWith("java/sql/")
                    || (getClassName().equals("java/util/concurrent/Future") && !method.getName().startsWith("is"))
                    || (getClassName().equals("java/lang/Process") && method.getName().equals("exitValue"))) {
                status = SideEffectStatus.SIDE_EFFECT;
            } else if(isObjectOnlyMethod(getMethodDescriptor())) {
                status = SideEffectStatus.OBJECT_ONLY;
            } else {
                String[] thrownExceptions = getXMethod().getThrownExceptions();
                if(thrownExceptions != null && thrownExceptions.length > 0) {
                    status = SideEffectStatus.SIDE_EFFECT;
                }
            }
        }
        if ((status == SideEffectStatus.SIDE_EFFECT || status == SideEffectStatus.OBJECT_ONLY) || method.isAbstract()
                || method.isInterface() || method.isNative()) {
            handleStatus();
        }
    }

    @Override
    public void visit(Field obj) {
        XField xField = getXField();
        if(!xField.isStatic() && (xField.isPrivate() || xField.isFinal()) && xField.isReferenceType()) {
            allowedFields.add(xField.getFieldDescriptor());
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        for(MethodDescriptor method : fieldsModifyingMethods) {
            List<MethodCall> calls = callGraph.get(method);
            SideEffectStatus prevStatus = statusMap.get(method);
            status = prevStatus.toSure();
            calledMethods = new ArrayList<>();
            for(MethodCall methodCall : calls) {
                FieldDescriptor target = methodCall.getTarget();
                if(target != TARGET_NEW && target != TARGET_OTHER && target != TARGET_THIS) {
                    if(allowedFields.contains(target)) {
                        methodCall = new MethodCall(methodCall.getMethod(), TARGET_THIS);
                    } else {
                        methodCall = new MethodCall(methodCall.getMethod(), TARGET_OTHER);
                    }
                }
                sawCall(methodCall, false);
                if(status == SideEffectStatus.SIDE_EFFECT) {
                    break;
                }
            }
            if (status != prevStatus) {
                statusMap.put(method, status);
            }
            if(status.unsure()) {
                calledMethods.trimToSize();
                callGraph.put(method, calledMethods);
            } else {
                callGraph.remove(method);
            }
        }
        MethodDescriptor clinit = new MethodDescriptor(getClassName(), "<clinit>", "()V", true);
        if(!statusMap.containsKey(clinit)) {
            status = SideEffectStatus.NO_SIDE_EFFECT;
            calledMethods = new ArrayList<>();
            superClinitCall();
            statusMap.put(clinit, status);
            if(status == SideEffectStatus.UNSURE || status == SideEffectStatus.UNSURE_OBJECT_ONLY) {
                calledMethods.trimToSize();
                callGraph.put(clinit, calledMethods);
            }
        }
    }

    private void superClinitCall() {
        ClassDescriptor superclassDescriptor = getXClass().getSuperclassDescriptor();
        if(superclassDescriptor != null && !superclassDescriptor.getClassName().equals("java/lang/Object")) {
            sawCall(new MethodCall(new MethodDescriptor(superclassDescriptor.getClassName(), "<clinit>", "()V", true), TARGET_THIS), false);
        }
    }

    private void handleStatus() {
        statusMap.put(getMethodDescriptor(), status);
        if(status == SideEffectStatus.UNSURE || status == SideEffectStatus.UNSURE_OBJECT_ONLY) {
            calledMethods.trimToSize();
            callGraph.put(getMethodDescriptor(), calledMethods);
        } else {
            fieldsModifyingMethods.remove(getMethodDescriptor());
        }
    }

    @Override
    public void visit(Code obj) {
        uselessVoidCandidate = !classInit && !constructor && !getXMethod().isSynthetic() && Type.getReturnType(getMethodSig()) == Type.VOID;
        byte[] code = obj.getCode();
        if(code.length == 4 && (code[0] & 0xFF) == GETSTATIC && (code[3] & 0xFF) == ARETURN) {
            getStaticMethods.add(getMethodDescriptor());
            handleStatus();
            return;
        }

        if (code.length <= 2 && !getXMethod().isStatic() && (getXMethod().isPublic() || getXMethod().isProtected())
                && !getXMethod().isFinal() && (getXClass().isPublic() || getXClass().isProtected())) {
            for(byte[] stubMethod : STUB_METHODS) {
                if (Arrays.equals(stubMethod, code)
                        && (getClassName().endsWith("Visitor") || getClassName().endsWith("Listener") || !hasOtherImplementations(getXMethod()))) {
                    // stub method which can be extended: assume it can be extended with possible side-effect
                    status = SideEffectStatus.SIDE_EFFECT;
                    handleStatus();
                    return;
                }
            }
        }
        if (statusMap.containsKey(getMethodDescriptor())) {
            return;
        }
        finallyTargets = new HashSet<>();
        for(CodeException ex : getCode().getExceptionTable()) {
            if(ex.getCatchType() == 0) {
                finallyTargets.add(ex.getHandlerPC());
            }
        }
        finallyExceptionRegisters = new HashSet<>();
        try {
            super.visit(obj);
        } catch (EarlyExitException e) {
            // Ignore
        }
        if (uselessVoidCandidate && code.length > 1
                && (status == SideEffectStatus.UNSURE || status == SideEffectStatus.NO_SIDE_EFFECT)) {
            uselessVoidCandidates.add(getMethodDescriptor());
        }
        handleStatus();
    }

    @Override
    public void sawOpcode(int seen) {
        if (!allowedFields.isEmpty() && seen == PUTFIELD) {
            Item objItem = getStack().getStackItem(1);
            if (objItem.getRegisterNumber() == 0) {
                if (allowedFields.contains(getFieldDescriptorOperand())) {
                    Item valueItem = getStack().getStackItem(0);
                    if (!isNew(valueItem) && !valueItem.isNull()) {
                        allowedFields.remove(getFieldDescriptorOperand());
                    }
                }
            }
        }
        if (status == SideEffectStatus.SIDE_EFFECT && allowedFields.isEmpty()) {
            // Nothing to do: skip the rest of the method
            throw new EarlyExitException();
        }
        if (status == SideEffectStatus.SIDE_EFFECT) {
            return;
        }
        switch (seen) {
        case ASTORE:
        case ASTORE_0:
        case ASTORE_1:
        case ASTORE_2:
        case ASTORE_3:
            if(finallyTargets.contains(getPC())) {
                finallyExceptionRegisters.add(getRegisterOperand());
            }
            break;
        case ATHROW: {
            Item exceptionItem = getStack().getStackItem(0);
            if(!finallyExceptionRegisters.remove(exceptionItem.getRegisterNumber())) {
                uselessVoidCandidate = false;
                try {
                    JavaClass javaClass = exceptionItem.getJavaClass();
                    if (javaClass != null && ALLOWED_EXCEPTIONS.contains(javaClass.getClassName())) {
                        break;
                    }
                } catch (ClassNotFoundException e) {
                }
                status = SideEffectStatus.SIDE_EFFECT;
            }
            break;
        }
        case PUTSTATIC:
            if(classInit) {
                if(getClassConstantOperand().equals(getClassName())) {
                    break;
                }
            }
            status = SideEffectStatus.SIDE_EFFECT;
            break;
        case INVOKEDYNAMIC:
            status = SideEffectStatus.SIDE_EFFECT;
            break;
        case PUTFIELD:
            sawCall(getMethodCall(FIELD_STORE_STUB_METHOD), false);
            break;
        case AASTORE:
        case DASTORE:
        case CASTORE:
        case BASTORE:
        case IASTORE:
        case LASTORE:
        case FASTORE:
        case SASTORE:
            sawCall(getMethodCall(ARRAY_STORE_STUB_METHOD), false);
            break;
        case INVOKESTATIC:
            if (changesOnlyNewObjects(getMethodDescriptorOperand())) {
                break;
            }
            sawCall(new MethodCall(getMethodDescriptorOperand(), TARGET_OTHER), false);
            break;
        case INVOKESPECIAL:
        case INVOKEINTERFACE:
        case INVOKEVIRTUAL: {
            XMethod xMethodOperand = getXMethodOperand();
            MethodDescriptor methodDescriptorOperand = xMethodOperand == null ? getMethodDescriptorOperand() : xMethodOperand
                    .getMethodDescriptor();
            if (changesOnlyNewObjects(getMethodDescriptorOperand())) {
                break;
            }
            MethodCall methodCall = getMethodCall(methodDescriptorOperand);
            sawCall(methodCall, false);
            break;
        }
        default:
            break;
        }
    }

    private MethodCall getMethodCall(MethodDescriptor methodDescriptorOperand) {
        Item objItem = getStack().getStackItem(getNumberArguments(methodDescriptorOperand.getSignature()));
        if (isNew(objItem)) {
            return new MethodCall(methodDescriptorOperand, TARGET_NEW);
        }
        if (objItem.getRegisterNumber() == 0 && !getMethod().isStatic()) {
            return new MethodCall(methodDescriptorOperand, constructor ? TARGET_NEW : TARGET_THIS);
        }
        XField xField = objItem.getXField();
        if (xField != null) {
            if (classInit && xField.isStatic() && xField.getClassDescriptor().getClassName().equals(getClassName())) {
                return new MethodCall(methodDescriptorOperand, TARGET_NEW);
            }
            if (!getMethodDescriptor().isStatic() && objItem.getFieldLoadedFromRegister() == 0
                    && allowedFields.contains(xField.getFieldDescriptor())) {
                fieldsModifyingMethods.add(getMethodDescriptor());
                return new MethodCall(methodDescriptorOperand, xField.getFieldDescriptor());
            }
        }
        return new MethodCall(methodDescriptorOperand, TARGET_OTHER);
    }

    private void sawCall(MethodCall methodCall, boolean finalPass) {
        if (status == SideEffectStatus.SIDE_EFFECT) {
            return;
        }
        MethodDescriptor methodDescriptor = methodCall.getMethod();
        if (hasNoSideEffect(methodDescriptor)) {
            sawNoSideEffectCall(methodDescriptor);
            return;
        }
        FieldDescriptor target = methodCall.getTarget();
        SideEffectStatus calledStatus = isObjectOnlyMethod(methodDescriptor) ? SideEffectStatus.OBJECT_ONLY : statusMap
                .get(methodDescriptor);
        if (calledStatus == null) {
            calledStatus = finalPass ? hasNoSideEffectUnknown(methodDescriptor) ? SideEffectStatus.NO_SIDE_EFFECT : SideEffectStatus.SIDE_EFFECT
                    : SideEffectStatus.UNSURE;
        }
        switch(calledStatus) {
        case NO_SIDE_EFFECT:
            sawNoSideEffectCall(methodDescriptor);
            return;
        case SIDE_EFFECT:
            status = SideEffectStatus.SIDE_EFFECT;
            return;
        case OBJECT_ONLY:
            if(target == TARGET_THIS) {
                status = status.toObjectOnly();
            } else if(target == TARGET_OTHER) {
                status = SideEffectStatus.SIDE_EFFECT;
            } else if(target != TARGET_NEW) {
                status = status.toObjectOnly();
                sawUnsureCall(methodCall);
            }
            return;
        case UNSURE_OBJECT_ONLY:
            if(target == TARGET_NEW) {
                sawUnsureCall(methodCall);
            } else if(target == TARGET_OTHER) {
                status = SideEffectStatus.SIDE_EFFECT;
            } else {
                status = status.toObjectOnly();
                sawUnsureCall(methodCall);
            }
            return;
        case UNSURE:
            sawUnsureCall(methodCall);
            return;
        }
    }

    /**
     * @param methodDescriptor
     */
    private void sawNoSideEffectCall(MethodDescriptor methodDescriptor) {
        if(uselessVoidCandidate && Type.getReturnType(methodDescriptor.getSignature()) == Type.VOID
                && !methodDescriptor.getName().equals("<init>")) {
            /* To reduce false-positives we do not mark method as useless void if it calls
             * another useless void method. If that another method also in the scope of our project
             * then we will report it instead. If there's a cycle of no-side-effect calls, then
             * it's probably some delegation pattern and methods can be extended in future/derived
             * projects to do something useful.
             */
            uselessVoidCandidate = false;
        }
    }

    private void sawUnsureCall(MethodCall methodCall) {
        calledMethods.add(methodCall);
        status = status.toUnsure();
    }

    /**
     * @param item stack item to check
     * @return true if this stack item is known to be newly created
     */
    private static boolean isNew(OpcodeStack.Item item) {
        if(item.isNewlyAllocated()) {
            return true;
        }
        XMethod returnValueOf = item.getReturnValueOf();
        if(returnValueOf == null) {
            return false;
        }
        if("iterator".equals(returnValueOf.getName())
                && "()Ljava/util/Iterator;".equals(returnValueOf.getSignature())
                && Subtypes2.instanceOf(returnValueOf.getClassName(), "java.lang.Iterable")) {
            return true;
        }
        if(returnValueOf.getClassName().startsWith("[") && returnValueOf.getName().equals("clone")) {
            return true;
        }
        if(NEW_OBJECT_RETURNING_METHODS.contains(returnValueOf.getMethodDescriptor())) {
            return true;
        }
        return false;
    }

    private boolean changesOnlyNewObjects(MethodDescriptor methodDescriptor) {
        int arg = changedArg(methodDescriptor);
        if(arg == -1) {
            return false;
        }
        int nArgs = getNumberArguments(methodDescriptor.getSignature());
        if(!isNew(getStack().getStackItem(nArgs-arg-1))) {
            return false;
        }
        return true;
    }

    /**
     * @param m method to check
     * @return array of argument numbers (0-based) which this method writes into or null if we don't know anything about this method
     */
    private static int changedArg(MethodDescriptor m) {
        if(m.equals(ARRAY_COPY)) {
            return 2;
        }
        if(m.getName().equals("toArray") && m.getSignature().equals("([Ljava/lang/Object;)[Ljava/lang/Object;")
                && Subtypes2.instanceOf(m.getClassDescriptor(), "java.util.Collection")) {
            return 0;
        }
        if ((m.getName().equals("sort") || m.getName().equals("fill") || m.getName().equals("reverse") || m.getName().equals(
                "shuffle"))
                && (m.getSlashedClassName().equals("java/util/Arrays") || m.getSlashedClassName().equals("java/util/Collections"))) {
            return 0;
        }
        return -1;
    }

    /**
     * @param m method to check
     * @return true if given method is known to have no side effects
     */
    private static boolean hasNoSideEffect(MethodDescriptor m) {
        String className = m.getSlashedClassName();
        if("java/lang/String".equals(className)) {
            return !(m.getName().equals("getChars") || (m.getName().equals("getBytes") && m.getSignature().equals("(II[BI)V")));
        }
        if("java/lang/Math".equals(className)) {
            return !m.getName().equals("random");
        }
        if("java/lang/Throwable".equals(className)) {
            return m.getName().startsWith("get");
        }
        if("java/lang/Character".equals(className)) {
            return !m.getName().equals("toChars");
        }
        if("java/lang/Class".equals(className) && m.getName().startsWith("is")) {
            return true;
        }
        if("java/awt/Color".equals(className) && m.getName().equals("<init>")) {
            return true;
        }
        if("java/util/regex/Pattern".contains(className)) {
            // Pattern.compile is often used to check the PatternSyntaxException, thus we consider it as side-effect method
            return !m.getName().equals("compile") && !m.getName().equals("<init>");
        }
        if(className.startsWith("[") && m.getName().equals("clone")) {
            return true;
        }
        if(className.startsWith("org/w3c/dom/") && (m.getName().startsWith("get") || m.getName().startsWith("has") || m.getName().equals("item"))) {
            return true;
        }
        if(className.startsWith("java/util/") &&
                (className.endsWith("Set") || className.endsWith("Map") || className.endsWith("Collection")
                        || className.endsWith("List") || className.endsWith("Queue") || className.endsWith("Deque")
                        || className.endsWith("Vector")) || className.endsWith("Hashtable") || className.endsWith("Dictionary")) {
            // LinkedHashSet in accessOrder mode changes internal state during get/getOrDefault
            if(className.equals("java/util/LinkedHashMap") && m.getName().startsWith("get")) {
                return false;
            }
            if(NO_SIDE_EFFECT_COLLECTION_METHODS.contains(m.getName()) || (m.getName().equals("toArray") && m.getSignature().equals("()[Ljava/lang/Object;"))) {
                return true;
            }
        }
        if(m.getName().equals("binarySearch") && (m.getSlashedClassName().equals("java/util/Arrays") || m.getSlashedClassName().equals("java/util/Collections"))) {
            return true;
        }
        if(m.getName().startsWith("$SWITCH_TABLE$")) {
            return true;
        }
        if(m.getName().equals("<init>") && isObjectOnlyClass(className)) {
            return true;
        }
        if(m.getName().equals("toString") && m.getSignature().equals("()Ljava/lang/String;") && m.getSlashedClassName().startsWith("java/")) {
            return true;
        }
        if(NUMBER_CLASSES.contains(className)) {
            return !m.getSignature().startsWith("(Ljava/lang/String;");
        }
        if(!m.isStatic() && m.getName().equals("equals") &&
                m.getSignature().equals("(Ljava/lang/Object;)Z")) {
            return true;
        }
        if(NO_SIDE_EFFECT_METHODS.contains(m)) {
            return true;
        }
        return false;
    }

    /**
     * @param m method to check
     * @return true if we may assume that given unseen method has no side effect
     */
    private static boolean hasNoSideEffectUnknown(MethodDescriptor m) {
        if(m.isStatic() && m.getName().equals("<clinit>")) {
            // No side effect for class initializer of unseen class
            return true;
        }
        if(!m.isStatic() && m.getName().equals("toString") && m.getSignature().equals("()Ljava/lang/String;")) {
            // We assume no side effect for unseen toString methods
            return true;
        }
        if(!m.isStatic() && m.getName().equals("hashCode") && m.getSignature().equals("()I")) {
            // We assume no side effect for unseen hashCode methods
            return true;
        }
        if(m.isStatic() && m.getName().equals("values") && m.getSignature().startsWith("()")) {
            // We assume no side effect for unseen enums
            return Subtypes2.instanceOf(m.getClassDescriptor(), "java.lang.Enum");
        }
        return false;
    }

    /**
     * @param m method to check
     * @return true if given method is known to change its object only
     */
    private static boolean isObjectOnlyMethod(MethodDescriptor m) {
        if (m.isStatic() || m.getName().equals("<init>") || m.getName().equals("forEach")) {
            return false;
        }
        String className = m.getSlashedClassName();
        if(isObjectOnlyClass(className)) {
            return true;
        }
        if(className.startsWith("javax/xml/") && m.getName().startsWith("next")) {
            return true;
        }
        if ((className.startsWith("java/net/") || className.startsWith("javax/servlet"))
                && (m.getName().startsWith("remove") || m.getName().startsWith("add") || m.getName().startsWith("set"))) {
            return true;
        }
        if(OBJECT_ONLY_METHODS.contains(m)) {
            return true;
        }
        return false;
    }

    /**
     * @param className class to check
     * @return true if all methods of this class are known to be object-only or no-side-effect
     */
    private static boolean isObjectOnlyClass(String className) {
        if(OBJECT_ONLY_CLASSES.contains(className)) {
            return true;
        }
        if(className.startsWith("java/lang/") && (className.endsWith("Error") || className.endsWith("Exception"))) {
            return true;
        }
        return className.startsWith("java/util/") &&
                (className.endsWith("Set") || className.endsWith("Map") || className.endsWith("Collection")
                        || className.endsWith("List") || className.endsWith("Queue") || className.endsWith("Deque")
                        || className.endsWith("Vector"));
    }

    @Override
    public void report() {
        computeFinalStatus();
        Set<String> sideEffectClinit = new HashSet<>();
        for(Entry<MethodDescriptor, SideEffectStatus> entry : statusMap.entrySet()) {
            if (entry.getValue() == SideEffectStatus.SIDE_EFFECT && entry.getKey().isStatic() && entry.getKey().getName().equals("<clinit>")) {
                sideEffectClinit.add(entry.getKey().getSlashedClassName());
            }
        }
        for(Entry<MethodDescriptor, SideEffectStatus> entry : statusMap.entrySet()) {
            MethodDescriptor m = entry.getKey();
            if (entry.getValue() == SideEffectStatus.NO_SIDE_EFFECT) {
                String returnType = new SignatureParser(m.getSignature()).getReturnTypeSignature();
                if (!returnType.equals("V") || m.getName().equals("<init>")) {
                    if(m.equals(GET_CLASS)) {
                        /* We do not mark getClass() call as pure, because it can appear in code like this:
                            public class Outer {
                              public class Inner {}
                              public void test(Outer n) { n.new Inner(); }
                            }
                            The test method is compiled into (assumably it's done to generate NPE if n is null)
                               0: new           #16                 // class a/Outer$Inner
                               3: aload_1
                               4: dup
                               5: invokevirtual #18                 // Method java/lang/Object.getClass:()Ljava/lang/Class;
                               8: pop
                               9: invokespecial #22                 // Method a/Outer$Inner."<init>":(La/Outer;)V
                              12: return
                            So we would have a false-positive here
                         */
                        continue;
                    }
                    if (m.getName().startsWith("access$") && (!(m instanceof XMethod) || ((XMethod)m).getAccessMethodForMethod() == null)) {
                        /* We skip field access methods, because they can unnecessarily be used for static calls
                         * (probably by older javac)
                         */
                        continue;
                    }
                    if (m.getName().startsWith("jjStopStringLiteral")) {
                        /* Some old JJTree versions may generate redundant calls to this method
                         * Skip it as reports in generated code don't help much
                         */
                        continue;
                    }
                    if (m.isStatic() || m.getName().equals("<init>")) {
                        if(sideEffectClinit.contains(m.getSlashedClassName())) {
                            /* Skip static methods and constructors for classes which have
                             * side-effect class initializer
                             */
                            noSideEffectMethods.add(m, MethodSideEffectStatus.SE_CLINIT);
                            continue;
                        }
                    }
                    if(m.equals(CLASS_GET_NAME) // used sometimes to trigger class loading
                            || m.equals(HASH_CODE) // found intended hashCode call several times in different projects, need further research
                            ) {
                        noSideEffectMethods.add(m, MethodSideEffectStatus.NSE_EX);
                        continue;
                    }
                    if (m.isStatic() && getStaticMethods.contains(m) && !m.getSlashedClassName().startsWith("java/")) {
                        String returnSlashedClassName = ClassName.fromFieldSignature(returnType);
                        if(returnSlashedClassName != null) {
                            String returnClass = ClassName.toDottedClassName(returnSlashedClassName);
                            if(ClassName.extractPackageName(returnClass).equals(m.getClassDescriptor().getPackageName())) {
                                /* Skip methods which only retrieve static field from the same package
                                 * As they as often used to trigger class initialization
                                 */
                                noSideEffectMethods.add(m, MethodSideEffectStatus.NSE_EX);
                                continue;
                            }
                        }
                    }
                    noSideEffectMethods.add(m, MethodSideEffectStatus.NSE);
                } else {    // void methods
                    if(uselessVoidCandidates.contains(m)) {
                        if(m.getName().equals("maybeForceBuilderInitialization") && m.getSignature().equals("()V")) {
                            // Autogenerated by Google protocol buffer compiler
                            continue;
                        }
                        noSideEffectMethods.add(m, MethodSideEffectStatus.USELESS);
                    }
                }
            } else if(entry.getValue() == SideEffectStatus.OBJECT_ONLY) {
                noSideEffectMethods.add(m, MethodSideEffectStatus.OBJ);
            }
        }
    }

    /**
     * @param xMethod
     * @return true if this has other implementations
     */
    private static boolean hasOtherImplementations(XMethod xMethod) {
        Set<XMethod> superMethods = Hierarchy2.findSuperMethods(xMethod);
        superMethods.add(xMethod);
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        Set<ClassDescriptor> subtypes = new HashSet<>();
        for(XMethod superMethod : superMethods) {
            try {
                subtypes.addAll(subtypes2.getSubtypes(superMethod.getClassDescriptor()));
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        subtypes.remove(xMethod.getClassDescriptor());
        for (ClassDescriptor subtype : subtypes) {
            try {
                XClass xClass = subtype.getXClass();
                XMethod subMethod = xClass.findMatchingMethod(xMethod.getMethodDescriptor());
                if (subMethod != null) {
                    if(!subMethod.isAbstract() ) {
                        return true;
                    }
                }
            } catch (CheckedAnalysisException e) {
                // ignore
            }
        }
        return false;
    }

    private void computeFinalStatus() {
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<Entry<MethodDescriptor, List<MethodCall>>> iterator = callGraph.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<MethodDescriptor, List<MethodCall>> entry = iterator.next();
                MethodDescriptor method = entry.getKey();
                uselessVoidCandidate = uselessVoidCandidates.contains(method);
                SideEffectStatus prevStatus = statusMap.get(method);
                status = prevStatus.toSure();
                calledMethods = new ArrayList<>();
                for(MethodCall methodCall : entry.getValue()) {
                    sawCall(methodCall, true);
                    if(status == SideEffectStatus.SIDE_EFFECT) {
                        break;
                    }
                }
                if (!uselessVoidCandidate || (status != SideEffectStatus.UNSURE && status != SideEffectStatus.NO_SIDE_EFFECT)) {
                    uselessVoidCandidates.remove(method);
                }
                if (status != prevStatus || !entry.getValue().equals(calledMethods)) {
                    statusMap.put(method, status);
                    if (status.unsure()) {
                        entry.setValue(calledMethods);
                    } else {
                        iterator.remove();
                    }
                    changed = true;
                }
            }
        }
        for(Entry<MethodDescriptor, List<MethodCall>> entry : callGraph.entrySet()) {
            MethodDescriptor method = entry.getKey();
            status = statusMap.get(method);
            if(status == SideEffectStatus.UNSURE) {
                boolean safeCycle = true;
                for(MethodCall methodCall : entry.getValue()) {
                    SideEffectStatus calledStatus = statusMap.get(methodCall.getMethod());
                    if(calledStatus != SideEffectStatus.UNSURE && calledStatus != SideEffectStatus.NO_SIDE_EFFECT) {
                        safeCycle = false;
                        break;
                    }
                }
                if(safeCycle) {
                    statusMap.put(method, SideEffectStatus.NO_SIDE_EFFECT);
                    uselessVoidCandidate = uselessVoidCandidates.contains(method);
                    if(uselessVoidCandidate) {
                        for(MethodCall call : entry.getValue()) {
                            uselessVoidCandidate = false;
                            if((call.getMethod().equals(method) && call.getTarget() == TARGET_THIS) || method.isStatic()) {
                                uselessVoidCandidate = true;
                            } else {
                                if(call.getMethod() instanceof XMethod) {
                                    XMethod xMethod = (XMethod) call.getMethod();
                                    if(xMethod.isFinal() || (!xMethod.isPublic() && !xMethod.isProtected())) {
                                        uselessVoidCandidate = true;
                                    }
                                }
                            }
                            if(!uselessVoidCandidate) {
                                break;
                            }
                        }
                        if(!uselessVoidCandidate) {
                            uselessVoidCandidates.remove(method);
                        }
                    }
                }
            }
        }
    }
}
