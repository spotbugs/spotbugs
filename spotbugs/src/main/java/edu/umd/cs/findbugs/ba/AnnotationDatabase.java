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

package edu.umd.cs.findbugs.ba;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Database to keep track of annotated fields/methods/classes/etc. for a
 * particular kind of annotation.
 *
 * @author William Pugh
 */
public class AnnotationDatabase<AnnotationEnum extends AnnotationEnumeration<AnnotationEnum>> {
    static final boolean DEBUG = SystemProperties.getBoolean("annotations.debug");

    public static final boolean IGNORE_BUILTIN_ANNOTATIONS = SystemProperties.getBoolean("findbugs.ignoreBuiltinAnnotations");

    public static enum Target {
        FIELD, METHOD, PARAMETER, @Deprecated
        CLASS, ANY
    }

    //    private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

    private final Map<Object, AnnotationEnum> directAnnotations = new HashMap<Object, AnnotationEnum>();

    private final Map<AnnotationDatabase.Target, Map<String, AnnotationEnum>> defaultAnnotation = new EnumMap<AnnotationDatabase.Target, Map<String, AnnotationEnum>>(AnnotationDatabase.Target.class);

    // private Subtypes subtypes;
    public AnnotationDatabase() {
        defaultAnnotation.put(Target.ANY, new HashMap<String, AnnotationEnum>());
        defaultAnnotation.put(Target.PARAMETER, new HashMap<String, AnnotationEnum>());
        defaultAnnotation.put(Target.METHOD, new HashMap<String, AnnotationEnum>());
        defaultAnnotation.put(Target.FIELD, new HashMap<String, AnnotationEnum>());
        // if (!Subtypes.DO_NOT_USE) {
        // subtypes = AnalysisContext.currentAnalysisContext().getSubtypes();
        // }

    }

    public void loadAuxiliaryAnnotations() {

    }

    private final Set<AnnotationEnum> seen = new HashSet<AnnotationEnum>();

    public void addDirectAnnotation(Object o, AnnotationEnum n) {
        directAnnotations.put(o, n);
        seen.add(n);
    }

    public void addDefaultAnnotation(Target target, String c, AnnotationEnum n) {
        if (!defaultAnnotation.containsKey(target)) {
            return;
        }
        if (DEBUG) {
            System.out.println("Default annotation " + target + " " + c + " " + n);
        }
        defaultAnnotation.get(target).put(c, n);
        seen.add(n);
    }

    public boolean anyAnnotations(AnnotationEnum n) {
        return seen.contains(n);
    }

    // TODO: Parameterize these values?
    Map<Object, AnnotationEnum> cachedMinimal = new MapCache<Object, AnnotationEnum>(20000);

    Map<Object, AnnotationEnum> cachedMaximal = new MapCache<Object, AnnotationEnum>(20000);

    @CheckForNull
    public AnnotationEnum getResolvedAnnotation(Object o, boolean getMinimal) {
        if (o instanceof XMethod) {
            XMethod m = (XMethod) o;
            if (m.getName().startsWith("access$")) {
                InnerClassAccessMap icam = AnalysisContext.currentAnalysisContext().getInnerClassAccessMap();
                try {
                    InnerClassAccess ica = icam.getInnerClassAccess(m.getClassName(), m.getName());
                    if (ica != null && ica.isLoad()) {
                        o = ica.getField();
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                    return null;
                }

            }
        }
        Map<Object, AnnotationEnum> cache;
        if (getMinimal) {
            cache = cachedMinimal;
        } else {
            cache = cachedMaximal;
        }

        if (cache.containsKey(o)) {
            return cache.get(o);
        }
        AnnotationEnum n = getUncachedResolvedAnnotation(o, getMinimal);
        if (DEBUG) {
            System.out.println("TTT: " + o + " " + n);
        }
        cache.put(o, n);
        return n;
    }

    public boolean annotationIsDirect(Object o) {
        return directAnnotations.containsKey(o);
    }

    @CheckForNull
    public AnnotationEnum getUncachedResolvedAnnotation(final Object o, boolean getMinimal) {

        AnnotationEnum n = getDirectAnnotation(o);
        if (n != null) {
            return n;
        }

        try {

            String className;
            Target kind;
            boolean isParameterToInitMethodofAnonymousInnerClass = false;
            boolean isSyntheticMethod = false;
            if (o instanceof XMethod || o instanceof XMethodParameter) {

                XMethod m;
                if (o instanceof XMethod) {
                    m = (XMethod) o;
                    isSyntheticMethod = m.isSynthetic();
                    kind = Target.METHOD;
                    className = m.getClassName();
                } else if (o instanceof XMethodParameter) {
                    m = ((XMethodParameter) o).getMethod();
                    // Don't
                    isSyntheticMethod = m.isSynthetic();
                    className = m.getClassName();
                    kind = Target.PARAMETER;
                    if ("<init>".equals(m.getName())) {
                        int i = className.lastIndexOf('$');
                        if (i + 1 < className.length() && Character.isDigit(className.charAt(i + 1))) {
                            isParameterToInitMethodofAnonymousInnerClass = true;
                        }
                    }
                } else {
                    throw new IllegalStateException("impossible");
                }

                if (!m.isStatic() && !"<init>".equals(m.getName())) {
                    JavaClass c = Repository.lookupClass(className);
                    // get inherited annotation
                    TreeSet<AnnotationEnum> inheritedAnnotations = new TreeSet<AnnotationEnum>();
                    if (c.getSuperclassNameIndex() > 0) {

                        n = lookInOverriddenMethod(o, c.getSuperclassName(), m, getMinimal);
                        if (n != null) {
                            inheritedAnnotations.add(n);
                        }
                    }
                    for (String implementedInterface : c.getInterfaceNames()) {
                        n = lookInOverriddenMethod(o, implementedInterface, m, getMinimal);
                        if (n != null) {
                            inheritedAnnotations.add(n);
                        }
                    }
                    if (DEBUG) {
                        System.out.println("# of inherited annotations : " + inheritedAnnotations.size());
                    }
                    if (!inheritedAnnotations.isEmpty()) {
                        if (inheritedAnnotations.size() == 1) {
                            return inheritedAnnotations.first();
                        }
                        if (!getMinimal) {
                            return inheritedAnnotations.last();
                        }

                        AnnotationEnum min = inheritedAnnotations.first();
                        if (min.getIndex() == 0) {
                            inheritedAnnotations.remove(min);
                            min = inheritedAnnotations.first();
                        }
                        return min;
                    }
                    // check to see if method is defined in this class;
                    // if not, on't consider default annotations
                    if (!classDefinesMethod(c, m)) {
                        return null;
                    }
                    if (DEBUG) {
                        System.out.println("looking for default annotations: " + c.getClassName() + " defines " + m);
                    }
                } // if not static
            } // associated with method
            else if (o instanceof XField) {

                className = ((XField) o).getClassName();
                kind = Target.FIELD;
            } else if (o instanceof String) {
                assert false;
                className = (String) o;
                kind = Target.CLASS;
            } else {
                throw new IllegalArgumentException("Can't look up annotation for " + o.getClass().getName());
            }

            // <init> method parameters for inner classes don't inherit default
            // annotations
            // since some of them are synthetic
            if (isParameterToInitMethodofAnonymousInnerClass) {
                return null;
            }

            // synthetic elements should not inherit default annotations
            if (isSyntheticMethod) {
                return null;
            }
            try {
                XClass c = Global.getAnalysisCache().getClassAnalysis(XClass.class,
                        DescriptorFactory.createClassDescriptorFromDottedClassName(className));

                if (c != null && c.isSynthetic()) {
                    return null;
                }
            } catch (CheckedAnalysisException e) {
                assert true;
            }

            // look for default annotation
            n = defaultAnnotation.get(kind).get(className);
            if (DEBUG) {
                System.out.println("Default annotation for " + kind + " is " + n);
            }
            if (n != null) {
                return n;
            }

            n = defaultAnnotation.get(Target.ANY).get(className);
            if (DEBUG) {
                System.out.println("Default annotation for any is " + n);
            }
            if (n != null) {
                return n;
            }

            int p = className.lastIndexOf('.');
            className = className.substring(0, p + 1) + "package-info";
            n = defaultAnnotation.get(kind).get(className);
            if (DEBUG) {
                System.out.println("Default annotation for " + kind + " is " + n);
            }
            if (n != null) {
                return n;
            }

            n = defaultAnnotation.get(Target.ANY).get(className);
            if (DEBUG) {
                System.out.println("Default annotation for any is " + n);
            }
            if (n != null) {
                return n;
            }

            return n;
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return null;
        }

    }

    public AnnotationEnum getDirectAnnotation(final Object o) {
        return directAnnotations.get(o);
    }

    private boolean classDefinesMethod(JavaClass c, XMethod m) {
        for (Method definedMethod : c.getMethods()) {
            if (definedMethod.getName().equals(m.getName()) && definedMethod.getSignature().equals(m.getSignature())
                    && definedMethod.isStatic() == m.isStatic()) {
                return true;
            }
        }
        return false;
    }

    private AnnotationEnum lookInOverriddenMethod(final Object originalQuery, String classToLookIn, XMethod originalMethod,
            boolean getMinimal) {
        try {
            AnnotationEnum n;
            // Look in supermethod
            XMethod superMethod = XFactory.createXMethod(classToLookIn, originalMethod.getName(), originalMethod.getSignature(),
                    originalMethod.isStatic());
            if (!superMethod.isResolved()) {
                return null;
            }
            if (DEBUG) {
                System.out.println("Looking for overridden method " + superMethod);
            }

            Object probe;
            if (originalQuery instanceof XMethod) {
                probe = superMethod;
            } else if (originalQuery instanceof XMethodParameter) {
                probe = new XMethodParameter(superMethod, ((XMethodParameter) originalQuery).getParameterNumber());
            } else {
                throw new IllegalStateException("impossible");
            }

            n = getResolvedAnnotation(probe, getMinimal);
            return n;
        } catch (RuntimeException e) {
            AnalysisContext.logError("Exception while looking for annotation of " + originalMethod + "in " + classToLookIn, e);
            return null;
        }
    }

    boolean addClassOnly = false;

    public boolean setAddClassOnly(boolean newValue) {
        boolean oldValue = addClassOnly;
        addClassOnly = newValue;
        return oldValue;
    }

    protected void addDefaultMethodAnnotation(String cName, AnnotationEnum annotation) {
        // if (!Subtypes.DO_NOT_USE) {
        // subtypes.addNamedClass(cName);
        // }
        if (addClassOnly) {
            return;
        }
        addDefaultAnnotation(AnnotationDatabase.Target.METHOD, cName, annotation);
    }

    protected void addFieldAnnotation(String cName, String mName, String mSig, boolean isStatic, AnnotationEnum annotation) {
        // if (!Subtypes.DO_NOT_USE) {
        // subtypes.addNamedClass(cName);
        // }
        if (addClassOnly) {
            return;
        }
        XField m = XFactory.createXField(cName, mName, mSig, isStatic);
        addDirectAnnotation(m, annotation);
    }

    protected void addMethodAnnotation(Class<?> clazz, String mName, String mSig, boolean isStatic, AnnotationEnum annotation) {
        addMethodAnnotation(clazz.getName(), mName, mSig, isStatic, annotation);
    }

    protected void addMethodAnnotation(@DottedClassName String cName, String mName, String mSig, boolean isStatic, AnnotationEnum annotation) {
        if (addClassOnly) {
            return;
        }
        XMethod m = XFactory.createXMethod(cName, mName, mSig, isStatic);
        if (!m.getClassName().equals(cName)){
            return;
        }
        /*
        if (false && !m.isResolved()) {
            System.out.println("Unable to add annotation " + annotation + " to " + m);
            ClassDescriptor c = DescriptorFactory.createClassDescriptorFromDottedClassName(cName);
            if (true)
                try {
                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                    if (xClass != null) {
                        System.out.println("class has methods: ");
                        for (XMethod m2 : xClass.getXMethods())
                            System.out.println("  " + m2);
                    }
                } catch (CheckedAnalysisException e) {
                    e.printStackTrace();
                }
        }
         */
        addDirectAnnotation(m, annotation);
    }

    private boolean onlyAppliesToReferenceParameters(AnnotationEnum annotation) {
        // return annotation instanceof NullnessAnnotation; work around JDK bug
        return true;
    }

    protected void addMethodParameterAnnotation(String cName, String mName, String mSig, boolean isStatic, int param,
            AnnotationEnum annotation) {
        // if (!Subtypes.DO_NOT_USE) {
        // subtypes.addNamedClass(cName);
        // }
        if (addClassOnly) {
            return;
        }
        SignatureParser parser = new SignatureParser(mSig);
        if (param < 0 || param >= parser.getNumParameters()) {
            throw new IllegalArgumentException("can't annotation parameter #" + param + " of " + cName + "." + mName + mSig);
        }
        String signature = parser.getParameter(param);
        char firstChar = signature.charAt(0);
        boolean isReference = firstChar == 'L' || firstChar == '[';
        if (onlyAppliesToReferenceParameters(annotation) && !isReference) {
            AnalysisContext.logError("Can't apply " + annotation + " to parameter " + param + " with signature " + signature
                    + " of " + cName + "." + mName + " : " + mSig);
            return;
        }
        XMethod m = XFactory.createXMethod(cName, mName, mSig, isStatic);
        addDirectAnnotation(new XMethodParameter(m, param), annotation);
    }
}
