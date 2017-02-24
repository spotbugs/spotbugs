/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.SelfMethodCalls;
import edu.umd.cs.findbugs.util.MultiMap;
import edu.umd.cs.findbugs.util.TopologicalSort;
import edu.umd.cs.findbugs.util.TopologicalSort.OutEdges2;
import edu.umd.cs.findbugs.util.Util;

/**
 * ClassInfo represents important metadata about a loaded class, such as its
 * superclass, access flags, codebase entry, etc.
 *
 * @author David Hovemeyer
 */
public class ClassInfo extends ClassNameAndSuperclassInfo implements XClass {

    private final static boolean DEBUG = SystemProperties.getBoolean("ci.debug");
    private final FieldInfo[] xFields;

    private final MethodInfo[] xMethods;

    private final MethodInfo[] methodsInCallOrder;

    private final ClassDescriptor immediateEnclosingClass;

    /* final */Map<ClassDescriptor, AnnotationValue> classAnnotations;

    final private String classSourceSignature;

    final private String source;

    private final boolean usesConcurrency;

    private final boolean hasStubs;

    @CheckForNull
    AnnotatedObject containingScope;

    private boolean containingScopeCached;

    public static class Builder extends ClassNameAndSuperclassInfo.Builder {
        private List<FieldInfo> fieldInfoList = new LinkedList<FieldInfo>();

        private List<MethodInfo> methodInfoList = new LinkedList<MethodInfo>();

        /**
         * Mapping from one method signature to its bridge method signature
         */
        private final Map<MethodInfo, String> bridgedSignatures = new IdentityHashMap<MethodInfo, String>();

        private ClassDescriptor immediateEnclosingClass;

        final Map<ClassDescriptor, AnnotationValue> classAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(3);

        private String classSourceSignature;

        private String source;

        boolean usesConcurrency;

        boolean hasStubs;

        private static String arguments(String signature) {
            int i = signature.indexOf('(');
            if (i == -1) {
                return signature;
            }
            return signature.substring(0,i+1);
        }

        @Override
        public ClassInfo build() {
            AnalysisContext context = AnalysisContext.currentAnalysisContext();
            FieldInfo fields[];
            MethodInfo methods[];
            if (fieldInfoList.size() == 0) {
                fields = FieldInfo.EMPTY_ARRAY;
            } else {
                fields = fieldInfoList.toArray(new FieldInfo[fieldInfoList.size()]);
            }


            for(MethodInfo m : methodInfoList) {
                if (m.isBridge() && !bridgedSignatures.containsKey(m))  {

                    if (DEBUG) {
                        System.out.println("Have bridge method:" + m);
                    }
                    for(MethodInfo to : methodInfoList) {
                        if (m != to) {
                            if (!to.isBridge()
                                    && m.getName().equals(to.getName())
                                    && arguments( m.getSignature()).equals(arguments(to.getSignature()))) {
                                if (DEBUG) {
                                    System.out.println("  to method:" + to);
                                }
                                bridgedSignatures.put(m, to.getSignature());
                            }

                        }
                    } // end  for(MethodInfo to
                } // end  if (m.isBridge()
            } // end  for(MethodInfo m : methodInfoList)



            for (Map.Entry<MethodInfo, String> e : bridgedSignatures.entrySet()) {
                MethodInfo method = e.getKey();
                String signature = e.getValue();
                for (MethodInfo m : methodInfoList) {
                    if (m.getName().equals(method.getName()) && m.getSignature().equals(signature)) {
                        if (DEBUG) {
                            System.out.println("Found bridge method:");
                            System.out.println("  " + method);
                            if (!method.getAnnotations().isEmpty()) {
                                System.out.println("    " + method.getAnnotations());
                            }
                            System.out.println("  " + m);
                            if (!m.getAnnotations().isEmpty()) {
                                System.out.println("    " + m.getAnnotations());
                            }
                        }
                        context.setBridgeMethod(method, m);

                    }
                }

            } // end for

            if (methodInfoList.size() == 0) {
                methods = MethodInfo.EMPTY_ARRAY;
            } else {
                methods = methodInfoList.toArray(new MethodInfo[methodInfoList.size()]);
            }

            return new ClassInfo(classDescriptor, classSourceSignature, superclassDescriptor, interfaceDescriptorList,
                    codeBaseEntry, accessFlags, source, majorVersion, minorVersion, referencedClassDescriptorList,
                    calledClassDescriptors, classAnnotations, fields, methods, immediateEnclosingClass, usesConcurrency,
                    hasStubs);
        }

        public void setSource(String source) {
            this.source = source;
        }

        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }

        public void setSourceSignature(String classSourceSignature) {
            this.classSourceSignature = classSourceSignature;
        }

        public void addAnnotation(String name, AnnotationValue value) {
            ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
            classAnnotations.put(annotationClass, value);
        }

        public void setFieldDescriptorList(FieldInfo[] fieldDescriptorList) {
            this.fieldInfoList = Arrays.asList(fieldDescriptorList);
        }

        public void addFieldDescriptor(FieldInfo field) {
            fieldInfoList.add(field);
        }

        public void setMethodDescriptorList(MethodInfo[] methodDescriptorList) {
            this.methodInfoList = Arrays.asList(methodDescriptorList);
        }

        public void addMethodDescriptor(MethodInfo method) {
            methodInfoList.add(method);
        }

        public void addBridgeMethodDescriptor(MethodInfo from, String bridgedSignature) {
            if (bridgedSignature != null) {
                bridgedSignatures.put(from, bridgedSignature);
            }
            addMethodDescriptor(from);
        }

        public void setImmediateEnclosingClass(ClassDescriptor immediateEnclosingClass) {
            this.immediateEnclosingClass = immediateEnclosingClass;
        }

        public void setUsesConcurrency() {
            usesConcurrency = true;
        }

        public void setHasStubs() {
            hasStubs = true;
        }

    }

    private MethodInfo[] computeMethodsInCallOrder() {
        final Map<String, MethodInfo> map = new HashMap<String, MethodInfo>();

        for (MethodInfo m : xMethods) {
            map.put(m.getName() + m.getSignature() + m.isStatic(), m);
        }
        final MultiMap<MethodInfo, MethodInfo> multiMap = SelfMethodCalls.getSelfCalls(getClassDescriptor(), map);
        OutEdges2<MethodInfo> edges1 = new OutEdges2<MethodInfo>() {

            @Override
            public Collection<MethodInfo> getOutEdges(MethodInfo method) {
                return multiMap.get(method);
            }

            @Override
            public int score(MethodInfo e) {
                return e.getMethodCallCount();
            }
        };
        List<MethodInfo> result = TopologicalSort.sortByCallGraph(Arrays.asList(xMethods), edges1);
        assert xMethods.length == result.size();
        return result.toArray(new MethodInfo[result.size()]);
    }

    /**
     *
     * @param classDescriptor
     *            ClassDescriptor representing the class name
     * @param superclassDescriptor
     *            ClassDescriptor representing the superclass name
     * @param interfaceDescriptorList
     *            ClassDescriptors representing implemented interface names
     * @param codeBaseEntry
     *            codebase entry class was loaded from
     * @param accessFlags
     *            class's access flags
     * @param referencedClassDescriptorList
     *            ClassDescriptors of all classes/interfaces referenced by the
     *            class
     * @param fieldDescriptorList
     *            FieldDescriptors of fields defined in the class
     * @param methodInfoList
     *            MethodDescriptors of methods defined in the class
     */
    private ClassInfo(ClassDescriptor classDescriptor, String classSourceSignature, ClassDescriptor superclassDescriptor,
            ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags, String source,
            int majorVersion, int minorVersion, Collection<ClassDescriptor> referencedClassDescriptorList,
            Set<ClassDescriptor> calledClassDescriptors, Map<ClassDescriptor, AnnotationValue> classAnnotations,
            FieldInfo[] fieldDescriptorList, MethodInfo[] methodInfoList, ClassDescriptor immediateEnclosingClass,
            boolean usesConcurrency, boolean hasStubs) {
        super(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags,
                referencedClassDescriptorList, calledClassDescriptors, majorVersion, minorVersion);
        this.source = source;
        this.classSourceSignature = classSourceSignature;
        if (fieldDescriptorList.length == 0) {
            fieldDescriptorList = FieldInfo.EMPTY_ARRAY;
        }
        this.xFields = fieldDescriptorList;
        this.xMethods = methodInfoList;
        this.immediateEnclosingClass = immediateEnclosingClass;
        this.classAnnotations = Util.immutableMap(classAnnotations);
        this.usesConcurrency = usesConcurrency;
        this.hasStubs = hasStubs;
        this.methodsInCallOrder = computeMethodsInCallOrder();
        /*
        if (false) {
            System.out.println("Methods in call order for " + classDescriptor);
            for (MethodInfo m : methodsInCallOrder) {
                System.out.println("  " + m);
            }
            System.out.println();
        }
         */
    }

    @Override
    public List<? extends XField> getXFields() {
        return Arrays.asList(xFields);
    }

    @Override
    public List<? extends XMethod> getXMethods() {
        return Arrays.asList(xMethods);
    }

    public List<? extends XMethod> getXMethodsInCallOrder() {
        return Arrays.asList(methodsInCallOrder);
    }

    @Override
    public XMethod findMethod(String methodName, String methodSig, boolean isStatic) {
        int hash = FieldOrMethodDescriptor.getNameSigHashCode(methodName, methodSig);
        for (MethodInfo mInfo : xMethods) {
            if (mInfo.getNameSigHashCode() == hash && mInfo.getName().equals(methodName)
                    && mInfo.getSignature().equals(methodSig) && mInfo.isStatic() == isStatic) {
                return mInfo;
            }
        }
        return null;
    }

    @Override
    public XMethod findMethod(MethodDescriptor descriptor) {
        if (!descriptor.getClassDescriptor().equals(this)) {
            throw new IllegalArgumentException();
        }
        return findMatchingMethod(descriptor);
    }

    @Override
    public XMethod findMatchingMethod(MethodDescriptor descriptor) {
        return findMethod(descriptor.getName(), descriptor.getSignature(), descriptor.isStatic());
    }

    @Override
    public XField findField(String name, String signature, boolean isStatic) {
        int hash = FieldOrMethodDescriptor.getNameSigHashCode(name, signature);
        for (FieldInfo fInfo : xFields) {
            if (fInfo.getNameSigHashCode() == hash && fInfo.getName().equals(name) && fInfo.getSignature().equals(signature)
                    && fInfo.isStatic() == isStatic) {
                return fInfo;
            }
        }
        try {
            if (getSuperclassDescriptor() == null) {
                return null;
            }
            XClass superClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, getSuperclassDescriptor());
            XField result = superClass.findField(name, signature, isStatic);
            if (result != null) {
                return result;
            }
            if (!isStatic) {
                return null;
            }
            ClassDescriptor[] interfaces = getInterfaceDescriptorList();
            for (ClassDescriptor implementedInterface : interfaces) {
                superClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, implementedInterface);
                result = superClass.findField(name, signature, isStatic);
                if (result != null) {
                    return result;
                }
            }
            return null;
        } catch (CheckedAnalysisException e) {
            return null;
        }
    }

    @Override
    public ClassDescriptor getImmediateEnclosingClass() {
        return immediateEnclosingClass;
    }

    @Override
    public String getPackageName() {
        String dottedClassName = getClassDescriptor().toDottedClassName();
        int lastDot = dottedClassName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        } else {
            return dottedClassName.substring(0, lastDot);
        }
    }

    public String getSlashedPackageName() {
        String slashedClassName = getClassDescriptor().getClassName();
        int lastSlash = slashedClassName.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        } else {
            return slashedClassName.substring(0, lastSlash);
        }
    }

    @Override
    public Collection<ClassDescriptor> getAnnotationDescriptors() {
        return classAnnotations.keySet();
    }

    @Override
    public Collection<AnnotationValue> getAnnotations() {
        return classAnnotations.values();
    }

    @Override
    public AnnotationValue getAnnotation(ClassDescriptor desc) {
        return classAnnotations.get(desc);
    }

    /**
     * Destructively add an annotation to the object. In general, this is not a
     * great idea, since it could cause the same class to appear to have
     * different annotations at different times. However, this method is
     * necessary for "built-in" annotations that FindBugs adds to system
     * classes. As long as we add such annotations early enough that nobody will
     * notice, we should be ok.
     *
     * @param annotationValue
     *            an AnnotationValue to add to the class
     */
    public void addAnnotation(AnnotationValue annotationValue) {
        HashMap<ClassDescriptor, AnnotationValue> updatedMap = new HashMap<ClassDescriptor, AnnotationValue>(classAnnotations);
        updatedMap.put(annotationValue.getAnnotationClass(), annotationValue);
        classAnnotations = Util.immutableMap(updatedMap);
    }

    @Override
    public ElementType getElementType() {
        if (getClassName().endsWith("package-info")) {
            return ElementType.PACKAGE;
        } else if (isAnnotation()) {
            return ElementType.ANNOTATION_TYPE;
        }
        return ElementType.TYPE;

    }

    @Override
    public @CheckForNull
    String getSource() {
        return source;
    }

    @Override
    public @CheckForNull
    AnnotatedObject getContainingScope() {
        if (!containingScopeCached) {
            containingScope = getContainingScope0();
            containingScopeCached = true;
        }
        return containingScope;
    }

    public @CheckForNull
    AnnotatedObject getContainingScope0() {
        try {
            if (immediateEnclosingClass != null) {
                return Global.getAnalysisCache().getClassAnalysis(XClass.class, getImmediateEnclosingClass());
            }
            if (getClassName().endsWith("package-info")) {
                return null;
            }
            ClassDescriptor p = DescriptorFactory.createClassDescriptor(getSlashedPackageName() + "/" + "package-info");
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, p);
        } catch (CheckedAnalysisException e) {
            return null;
        }
    }

    @Override
    public String getSourceSignature() {
        return classSourceSignature;
    }

    @Override
    public boolean usesConcurrency() {
        return usesConcurrency;
    }

    @Override
    public boolean hasStubs() {
        return hasStubs;
    }

}
