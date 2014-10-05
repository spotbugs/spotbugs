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
import java.util.IdentityHashMap;
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
        if (sz == 0) {
            return EMPTY_ARRAY;
        }
        return new MethodInfo[sz];
    }

    static public class Builder {
        int accessFlags;

        long variableHasName;
        long variableIsSynthetic;

        final @SlashedClassName
        String className;

        final String methodName, methodSignature;

        String[] exceptions;

        String methodSourceSignature;

        boolean isUnconditionalThrower;

        boolean isUnsupported;

        boolean usesConcurrency;

        boolean isStub;

        boolean hasBackBranch;

        boolean isIdentity;

        boolean usesInvokeDynamic;

        int methodCallCount;

        MethodDescriptor accessMethodForMethod;
        FieldDescriptor accessMethodForField;

        final Map<ClassDescriptor, AnnotationValue> methodAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(4);

        final Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations = new HashMap<Integer, Map<ClassDescriptor, AnnotationValue>>(
                4);

        @Override
        public String toString() {
            return "builder for " + className + "." + methodName + methodSignature;
        }
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
            if (p < 64) {
                variableHasName |= 1 << p;
            }
        }

        public void setVariableIsSynthetic(int p) {
            if (p < 64) {
                variableIsSynthetic |= 1 << p;
            }
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
            Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(0);
            if (map != null) {
                // not identity if it has an annotation
                return;
            }
            this.isIdentity = true;
        }

        public void setAccessFlags(int accessFlags) {
            this.accessFlags = accessFlags;
        }

        public void addAccessFlags(int accessFlags) {
            this.accessFlags |= accessFlags;
        }

        public void addAnnotation(String name, AnnotationValue value) {
            ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
            methodAnnotations.put(annotationClass, value);
        }

        public void addParameterAnnotation(int parameter, String name, AnnotationValue value) {
            ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
            Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(parameter);
            if (map == null) {
                map = new HashMap<ClassDescriptor, AnnotationValue>();
                methodParameterAnnotations.put(parameter, map);
            }
            map.put(annotationClass, value);
        }

        public MethodInfo build() {
            if (variableHasName != 0) {
                variableIsSynthetic |= (~variableHasName);
            }
            return new MethodInfo(className, methodName, methodSignature, methodSourceSignature, accessFlags,
                    isUnconditionalThrower, isUnsupported, usesConcurrency, hasBackBranch, isStub, isIdentity,
                    usesInvokeDynamic, methodCallCount, exceptions, accessMethodForMethod,
                    accessMethodForField, methodAnnotations, methodParameterAnnotations, variableIsSynthetic);
        }

        public void setIsUnconditionalThrower() {
            isUnconditionalThrower = true;

        }

        public void setUnsupported() {
            isUnsupported = true;

        }

        /**
         * @param methodCallCount
         */
        public void setNumberMethodCalls(int methodCallCount) {
            this.methodCallCount = methodCallCount;

        }

        /**
         *
         */
        public void setUsesInvokeDynamic() {
            usesInvokeDynamic = true;
        }
    }

    final int accessFlags;

    final long variableIsSynthetic;

    final int methodCallCount;

    final boolean usesConcurrency;

    final boolean hasBackBranch;

    final boolean isStub;

    final String methodSourceSignature;

    final @CheckForNull
    String[] exceptions;

    Map<ClassDescriptor, AnnotationValue> methodAnnotations;

    Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations;

    public static class MethodInfoDatabase {
        final IdentityHashMap<MethodInfo, Void> unconditionalThrowers = new IdentityHashMap<MethodInfo, Void>();
        final IdentityHashMap<MethodInfo, Void> unsupportedMethods = new IdentityHashMap<MethodInfo, Void>();
        final IdentityHashMap<MethodInfo, MethodDescriptor> accessMethodForMethod = new IdentityHashMap<MethodInfo, MethodDescriptor>();
        final IdentityHashMap<MethodInfo, FieldDescriptor> accessMethodForField = new IdentityHashMap<MethodInfo, FieldDescriptor>();
        final IdentityHashMap<MethodInfo, Void> identityMethods = new IdentityHashMap<MethodInfo, Void>();
        final IdentityHashMap<MethodInfo, Void> invokeDynamicMethods = new IdentityHashMap<MethodInfo, Void>();

    }

    /**
     * @return Returns the database.
     */
    static MethodInfoDatabase getDatabase() {
        return Global.getAnalysisCache().getDatabase(MethodInfoDatabase.class);
    }
    static IdentityHashMap<MethodInfo, Void> getUnconditionalthrowers() {
        return getDatabase().unconditionalThrowers;
    }

    static IdentityHashMap<MethodInfo, Void> getUnsupportedmethods() {
        return getDatabase().unsupportedMethods;
    }

    static IdentityHashMap<MethodInfo, MethodDescriptor> getAccessmethodformethod() {
        return getDatabase().accessMethodForMethod;
    }

    static IdentityHashMap<MethodInfo, FieldDescriptor> getAccessmethodforfield() {
        return getDatabase().accessMethodForField;
    }

    static IdentityHashMap<MethodInfo, Void> getIdentitymethods() {
        return getDatabase().identityMethods;
    }
    static public IdentityHashMap<MethodInfo, Void> getInvokeDynamicMethods() {
        return getDatabase().invokeDynamicMethods;
    }

    MethodInfo(@SlashedClassName String className, String methodName, String methodSignature, String methodSourceSignature,
            int accessFlags, boolean isUnconditionalThrower, boolean isUnsupported, boolean usesConcurrency,
            boolean hasBackBranch, boolean isStub, boolean isIdentity,
            boolean usesInvokeDynamic, int methodCallCount, @CheckForNull String[] exceptions,
            @CheckForNull MethodDescriptor accessMethodForMethod,
            @CheckForNull FieldDescriptor accessMethodForField,
            Map<ClassDescriptor, AnnotationValue> methodAnnotations, Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations, long variableIsSynthetic) {
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
        if (isUnconditionalThrower) {
            getUnconditionalthrowers().put(this, null);
        }
        if (isUnsupported) {
            getUnconditionalthrowers().put(this, null);
        }
        if (accessMethodForMethod != null) {
            getAccessmethodformethod().put(this, accessMethodForMethod);
        }
        if (accessMethodForField!= null) {
            getAccessmethodforfield().put(this, accessMethodForField);
        }
        if (isIdentity) {
            getIdentitymethods().put(this, null);
        }
        if (usesInvokeDynamic) {
            getInvokeDynamicMethods().put(this, null);
        }

        this.usesConcurrency = usesConcurrency;
        this.hasBackBranch = hasBackBranch;
        this.isStub = isStub;
        this.methodCallCount = methodCallCount;
        this.variableIsSynthetic = variableIsSynthetic;
    }

    @Override
    public @CheckForNull
    String[] getThrownExceptions() {
        return exceptions;
    }



    @Override
    public boolean isUnconditionalThrower() {
        return getUnconditionalthrowers().containsKey(this);
    }

    @Override
    public boolean isIdentity() {
        return getIdentitymethods().containsKey(this);
    }

    @Override
    public boolean usesInvokeDynamic() {
        return getInvokeDynamicMethods().containsKey(this);
    }


    @Override
    public boolean isUnsupported() {
        return getUnsupportedmethods().containsKey(this);
    }

    @Override
    public int getNumParams() {
        return new SignatureParser(getSignature()).getNumParameters();
    }

    @Override
    public boolean isVariableSynthetic(int param) {
        if (param >= 64) {
            return false;
        }
        return (variableIsSynthetic & (1 << param)) != 0;
    }

    public int getMethodCallCount() {
        return methodCallCount;
    }

    private boolean checkFlag(int flag) {
        return (accessFlags & flag) != 0;
    }

    @Override
    public boolean isNative() {
        return checkFlag(Constants.ACC_NATIVE);
    }

    @Override
    public boolean isAbstract() {
        return checkFlag(Constants.ACC_ABSTRACT);
    }

    @Override
    public boolean isSynchronized() {
        return checkFlag(Constants.ACC_SYNCHRONIZED);
    }

    @Override
    public boolean isBridge() {
        return checkFlag(Constants.ACC_BRIDGE);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isReturnTypeReferenceType()
     */
    @Override
    public boolean isReturnTypeReferenceType() {
        SignatureParser parser = new SignatureParser(getSignature());
        String returnTypeSig = parser.getReturnTypeSignature();
        return SignatureParser.isReferenceType(returnTypeSig);
    }

    @Override
    public @DottedClassName
    String getClassName() {
        return getClassDescriptor().toDottedClassName();
    }

    @Override
    public @DottedClassName
    String getPackageName() {
        return getClassDescriptor().getPackageName();
    }

    @Override
    public String getSourceSignature() {
        return methodSourceSignature;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getAccessFlags()
     */
    @Override
    public int getAccessFlags() {
        return accessFlags;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isFinal()
     */
    @Override
    public boolean isFinal() {
        return checkFlag(Constants.ACC_FINAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPrivate()
     */
    @Override
    public boolean isPrivate() {
        return checkFlag(Constants.ACC_PRIVATE);
    }

    @Override
    public boolean isDeprecated() {
        return checkFlag(Opcodes.ACC_DEPRECATED);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isProtected()
     */
    @Override
    public boolean isProtected() {
        return checkFlag(Constants.ACC_PROTECTED);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPublic()
     */
    @Override
    public boolean isPublic() {
        return checkFlag(Constants.ACC_PUBLIC);
    }

    @Override
    public boolean isSynthetic() {
        return checkFlag(Constants.ACC_SYNTHETIC);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isResolved()
     */
    @Override
    public boolean isResolved() {
        return true;
    }

    @Override
    public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return Collections.<ClassDescriptor> emptySet();
        }
        return map.keySet();
    }

    @Override
    public boolean hasParameterAnnotations() {
        return !methodParameterAnnotations.isEmpty();
    }

    @Override
    public @Nullable
    AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return null;
        }
        return map.get(desc);
    }

    @Override
    public Collection<AnnotationValue> getParameterAnnotations(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return Collections.<AnnotationValue> emptySet();
        }
        return map.values();
    }

    @Override
    public Collection<ClassDescriptor> getAnnotationDescriptors() {
        return methodAnnotations.keySet();
    }

    @Override
    public AnnotationValue getAnnotation(ClassDescriptor desc) {
        return methodAnnotations.get(desc);
    }

    @Override
    public Collection<AnnotationValue> getAnnotations() {

        Collection<AnnotationValue> result =  methodAnnotations.values();
        if (result.isEmpty() && isBridge()) {
            XMethod to = bridgeTo();
            if (to != null) {
                result = to.getAnnotations();
            }
        }
        return result;
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
    @Override
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
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#getMethodDescriptor()
     */
    @Override
    public MethodDescriptor getMethodDescriptor() {
        return this;
    }

    @Override
    public ElementType getElementType() {
        if ("<init>".equals(getName())) {
            return ElementType.CONSTRUCTOR;
        }
        return ElementType.METHOD;
    }

    @Override
    public @CheckForNull
    AnnotatedObject getContainingScope() {
        try {
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
        } catch (CheckedAnalysisException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isVarArgs()
     */
    @Override
    public boolean isVarArgs() {
        return checkFlag(Constants.ACC_VARARGS);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#usesConcurrency()
     */
    @Override
    public boolean usesConcurrency() {
        return usesConcurrency;
    }


    public boolean hasBackBranch() {
        return hasBackBranch;
    }
    @Override
    public boolean isStub() {
        return isStub;
    }

    @Override
    public @CheckForNull
    MethodDescriptor getAccessMethodForMethod() {
        return getAccessmethodformethod().get(this);
    }
    @Override
    public @CheckForNull
    FieldDescriptor getAccessMethodForField() {
        return getAccessmethodforfield().get(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#bridgeFrom()
     */
    @Override
    public XMethod bridgeFrom() {
        return AnalysisContext.currentAnalysisContext().getBridgeFrom(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#bridgeTo()
     */
    @Override
    public XMethod bridgeTo() {
        return AnalysisContext.currentAnalysisContext().getBridgeTo(this);

    }

    @Override
    public XMethod resolveAccessMethodForMethod() {
        MethodDescriptor access = getAccessMethodForMethod();
        if (access != null) {
            return XFactory.createXMethod(access);
        }
        return this;
    }
}
