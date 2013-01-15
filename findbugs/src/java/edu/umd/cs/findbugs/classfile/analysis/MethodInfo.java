/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.analysis;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.bcel.Constants;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ComparableMethod;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class MethodInfo extends MethodDescriptor implements XMethod {

    public static final MethodInfo[] EMPTY_ARRAY = new MethodInfo[0];

    public static MethodInfo[] newArray(int sz) {
        if (sz == 0)
            return EMPTY_ARRAY;
        return new MethodInfo[sz];
    }

    static public class Builder {
        private int accessFlags;

        private long variableHasName;
        private long variableIsSynthetic;

        private final @SlashedClassName
        String className;

        private final String methodName, methodSignature;

        private String[] exceptions;

        private String methodSourceSignature;

        private boolean isUnconditionalThrower;

        private boolean isUnsupported;

        private boolean usesConcurrency;

        private boolean isStub;

        private boolean hasBackBranch;

        private boolean isIdentity;

        private int methodCallCount;

        private MethodDescriptor accessMethodForMethod;
        private FieldDescriptor accessMethodForField;

        @CheckForNull
        private Map<ClassDescriptor, AnnotationValue> methodAnnotations;

        @CheckForNull
        private Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations;

        public Builder(@SlashedClassName String className, String methodName, String methodSignature, int accessFlags) {
            this.className = className;
            this.methodName = methodName;
            this.methodSignature = methodSignature;
            this.accessFlags = accessFlags;
        }

        public void setAccessMethodForMethod(String owner, String name, String sig, boolean isStatic) {
            accessMethodForMethod = new MethodDescriptor(owner, name, sig, isStatic);
        }
        public void setAccessMethodForField(String owner, String name, String sig, boolean isStatic) {
            accessMethodForField = new FieldDescriptor(owner, name, sig, isStatic);
        }

        public void setSourceSignature(String methodSourceSignature) {
            this.methodSourceSignature = methodSourceSignature;
        }

        public void setVariableHasName(int p) {
            if (p < 64)
                variableHasName |= 1 << p;
        }

        public void setVariableIsSynthetic(int p) {
            if (p < 64)
                variableIsSynthetic |= 1 << p;
        }

        public void setUsesConcurrency() {
            this.usesConcurrency = true;
        }

        public void setIsStub() {
            this.isStub = true;
        }

        public void setHasBackBranch() {
            this.hasBackBranch = true;
        }

        public void setThrownExceptions(String[] exceptions) {
            this.exceptions = exceptions;
        }

        public void setIsIdentity() {
            if (!getMethodParameterAnnotations(false).isEmpty()) {
                // not identity if it has an annotation
                return;
            }
            this.isIdentity = true;
        }

        public void setAccessFlags(int accessFlags) {
            this.accessFlags = accessFlags;
        }

        public void addAccessFlags(int accessFlags1) {
            this.accessFlags |= accessFlags1;
        }

        public void addAnnotation(String name, AnnotationValue value) {
            ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
            getMethodAnnotations(true).put(annotationClass, value);
        }

        public void addParameterAnnotation(int parameter, String name, AnnotationValue value) {
            ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
            Map<Integer, Map<ClassDescriptor, AnnotationValue>> parameterAnnotations = getMethodParameterAnnotations(true);
            Map<ClassDescriptor, AnnotationValue> map = parameterAnnotations.get(parameter);
            if (map == null) {
                map = new HashMap<ClassDescriptor, AnnotationValue>();
                parameterAnnotations.put(parameter, map);
            }
            map.put(annotationClass, value);
        }

        public MethodInfo build() {
            if (variableHasName != 0) {
                variableIsSynthetic |= (~variableHasName);
            }
            return new MethodInfo(className, methodName, methodSignature, methodSourceSignature, accessFlags,
                    isUnconditionalThrower, isUnsupported, usesConcurrency, hasBackBranch, isStub, isIdentity,
                    methodCallCount, exceptions, accessMethodForMethod, accessMethodForField,
                    getMethodAnnotations(false), getMethodParameterAnnotations(false), variableIsSynthetic);
        }

        public void setIsUnconditionalThrower() {
            isUnconditionalThrower = true;

        }

        public void setUnsupported() {
            isUnsupported = true;

        }

        public void setNumberMethodCalls(int methodCallCount) {
            this.methodCallCount = methodCallCount;

        }

        Map<Integer, Map<ClassDescriptor, AnnotationValue>> getMethodParameterAnnotations(boolean create) {
            if(create && methodParameterAnnotations == null){
                methodParameterAnnotations = new HashMap<Integer, Map<ClassDescriptor, AnnotationValue>>(4);
            }
            return methodParameterAnnotations == null? Collections.EMPTY_MAP : methodParameterAnnotations;
        }

        Map<ClassDescriptor, AnnotationValue> getMethodAnnotations(boolean create) {
            if(create && methodAnnotations == null){
                methodAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(4);
            }
            return methodAnnotations == null? Collections.EMPTY_MAP : methodAnnotations;
        }
    }

    private final int accessFlags;

    private final long variableIsSynthetic;

    private final int methodCallCount;

    private final boolean usesConcurrency;

    private final boolean hasBackBranch;

    private final boolean isStub;

    private final String methodSourceSignature;

    private final @CheckForNull
    String[] exceptions;

    private Map<ClassDescriptor, AnnotationValue> methodAnnotations;

    private Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations;

    private final boolean unconditionalThrowers;
    private final boolean unsupportedMethods;
    private final MethodDescriptor accessMethodForMethod;
    private final FieldDescriptor accessMethodForField;
    private final boolean identifyMethods;

    MethodInfo(@SlashedClassName String className, String methodName, String methodSignature, String methodSourceSignature,
            int accessFlags, boolean isUnconditionalThrower, boolean isUnsupported, boolean usesConcurrency,
            boolean hasBackBranch, boolean isStub, boolean isIdentity,
            int methodCallCount, @CheckForNull String[] exceptions, @CheckForNull MethodDescriptor accessMethodForMethod,
            @CheckForNull FieldDescriptor accessMethodForField,
            Map<ClassDescriptor, AnnotationValue> methodAnnotations,
            Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations, long variableIsSynthetic) {
        super(className, methodName, methodSignature, (accessFlags & Constants.ACC_STATIC) != 0);
        this.accessFlags = accessFlags;
        this.exceptions = exceptions;
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; i++) {
                exceptions[i] = DescriptorFactory.canonicalizeString(exceptions[i]);
            }
        }
        this.methodSourceSignature = DescriptorFactory.canonicalizeString(methodSourceSignature);
        this.methodAnnotations = Util.immutableMap(methodAnnotations);
        this.methodParameterAnnotations = Util.immutableMap(methodParameterAnnotations);

        this.unconditionalThrowers = isUnconditionalThrower;
        this.unsupportedMethods = isUnsupported;
        this.accessMethodForMethod = accessMethodForMethod;
        this.accessMethodForField = accessMethodForField;
        this.identifyMethods = isIdentity;

        this.usesConcurrency = usesConcurrency;
        this.hasBackBranch = hasBackBranch;
        this.isStub = isStub;
        this.methodCallCount = methodCallCount;
        this.variableIsSynthetic = variableIsSynthetic;
    }

    public @CheckForNull
    String[] getThrownExceptions() {
        return exceptions;
    }

    public boolean isUnconditionalThrower() {
        return unconditionalThrowers;
    }

    public boolean isIdentity() {
        return identifyMethods;
    }

    public boolean isUnsupported() {
        return unsupportedMethods;
    }

    public int getNumParams() {
        return new SignatureParser(getSignature()).getNumParameters();
    }

    public boolean isVariableSynthetic(int param) {
        if (param >= 64) return false;
        return (variableIsSynthetic & (1 << param)) != 0;
    }

    public int getMethodCallCount() {
        return methodCallCount;
    }

    private boolean checkFlag(int flag) {
        return (accessFlags & flag) != 0;
    }

    public boolean isNative() {
        return checkFlag(Constants.ACC_NATIVE);
    }

    public boolean isAbstract() {
        return checkFlag(Constants.ACC_ABSTRACT);
    }

    public boolean isSynchronized() {
        return checkFlag(Constants.ACC_SYNCHRONIZED);
    }

    public boolean isReturnTypeReferenceType() {
        SignatureParser parser = new SignatureParser(getSignature());
        String returnTypeSig = parser.getReturnTypeSignature();
        return SignatureParser.isReferenceType(returnTypeSig);
    }

    public @DottedClassName
    String getClassName() {
        return getClassDescriptor().toDottedClassName();
    }

    public @DottedClassName
    String getPackageName() {
        return getClassDescriptor().getPackageName();
    }

    public String getSourceSignature() {
        return methodSourceSignature;
    }

    @Override
    public int compareTo(ComparableMethod rhs) {
        if (rhs instanceof MethodDescriptor) {
            return FieldOrMethodDescriptor.compareTo(this, (MethodDescriptor) rhs);
        }

        if (rhs instanceof XMethod) {
            return XFactory.compare((XMethod) this, (XMethod) rhs);
        }

        throw new ClassCastException("Can't compare a " + this.getClass().getName() + " to a " + rhs.getClass().getName());
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public boolean isFinal() {
        return checkFlag(Constants.ACC_FINAL);
    }

    public boolean isPrivate() {
        return checkFlag(Constants.ACC_PRIVATE);
    }

    public boolean isDeprecated() {
        return checkFlag(Opcodes.ACC_DEPRECATED);
    }

    public boolean isProtected() {
        return checkFlag(Constants.ACC_PROTECTED);
    }

    public boolean isPublic() {
        return checkFlag(Constants.ACC_PUBLIC);
    }

    public boolean isSynthetic() {
        return checkFlag(Constants.ACC_SYNTHETIC);
    }

    public boolean isResolved() {
        return true;
    }

    public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null)
            return Collections.<ClassDescriptor> emptySet();
        return map.keySet();
    }

    public @Nullable
    AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null)
            return null;
        return map.get(desc);
    }

    public Collection<AnnotationValue> getParameterAnnotations(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null)
            return Collections.<AnnotationValue> emptySet();
        return map.values();
    }

    public Collection<ClassDescriptor> getAnnotationDescriptors() {
        return methodAnnotations.keySet();
    }

    public AnnotationValue getAnnotation(ClassDescriptor desc) {
        return methodAnnotations.get(desc);
    }

    public Collection<AnnotationValue> getAnnotations() {
        return methodAnnotations.values();
    }

    /**
     * Destructively add an annotation. We do this for "built-in" annotations
     * that might not be directly evident in the code. It's not a great idea in
     * general, but we can get away with it as long as it's done early enough
     * (i.e., before anyone asks what annotations this method has.)
     *
     * @param annotationValue
     *            an AnnotationValue representing a method annotation
     */
    public void addAnnotation(AnnotationValue annotationValue) {
        HashMap<ClassDescriptor, AnnotationValue> updatedAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(
                methodAnnotations);
        updatedAnnotations.put(annotationValue.getAnnotationClass(), annotationValue);
        methodAnnotations = updatedAnnotations;
        TypeQualifierApplications.updateAnnotations(this);
    }

    /**
     * Destructively add a parameter annotation.
     *
     * @param param
     *            parameter (0 == first parameter)
     * @param annotationValue
     *            an AnnotationValue representing a parameter annotation
     */
    public void addParameterAnnotation(int param, AnnotationValue annotationValue) {
        HashMap<Integer, Map<ClassDescriptor, AnnotationValue>> updatedAnnotations = new HashMap<Integer, Map<ClassDescriptor, AnnotationValue>>(
                methodParameterAnnotations);
        Map<ClassDescriptor, AnnotationValue> paramMap = updatedAnnotations.get(param);
        if (paramMap == null) {
            paramMap = new HashMap<ClassDescriptor, AnnotationValue>();
            updatedAnnotations.put(param, paramMap);
        }
        paramMap.put(annotationValue.getAnnotationClass(), annotationValue);

        methodParameterAnnotations = updatedAnnotations;
        TypeQualifierApplications.updateAnnotations(this);
    }

    public MethodDescriptor getMethodDescriptor() {
        return this;
    }

    public ElementType getElementType() {
        if (getName().equals("<init>"))
            return ElementType.CONSTRUCTOR;
        return ElementType.METHOD;
    }

    public @CheckForNull
    AnnotatedObject getContainingScope() {
        try {
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
        } catch (CheckedAnalysisException e) {
            return null;
        }
    }

    public boolean isVarArgs() {
        return checkFlag(Constants.ACC_VARARGS);
    }

    public boolean usesConcurrency() {
        return usesConcurrency;
    }

    public boolean hasBackBranch() {
        return hasBackBranch;
    }
    public boolean isStub() {
        return isStub;
    }

    public @CheckForNull
    MethodDescriptor getAccessMethodForMethod() {
        return accessMethodForMethod;
    }
    public @CheckForNull
    FieldDescriptor getAccessMethodForField() {
        return accessMethodForField;
    }

    public XMethod bridgeFrom() {
        return AnalysisContext.currentAnalysisContext().getBridgeFrom(this);
    }

    public XMethod bridgeTo() {
        return AnalysisContext.currentAnalysisContext().getBridgeTo(this);

    }

    public XMethod resolveAccessMethodForMethod() {
        MethodDescriptor access = getAccessMethodForMethod();
        if (access != null)
            return XFactory.createXMethod(access);
        return this;
    }
}
