/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.meta.When;

import org.objectweb.asm.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.InnerClassAccess;
import edu.umd.cs.findbugs.ba.InnerClassAccessMap;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * Figure out where and how type qualifier annotations are applied.
 *
 * @author William Pugh
 * @author David Hovemeyer
 */
public class TypeQualifierApplications {
    static final boolean DEBUG = SystemProperties.getBoolean("ctq.applications.debug");

    static final String DEBUG_METHOD = SystemProperties.getProperty("ctq.applications.method");

    static final boolean DEBUG_DEFAULT_ANNOTATION = SystemProperties.getBoolean("ctq.applications.default.debug");

    /**
     * Should exclusive type qualifiers be handled?
     */
    static final boolean CHECK_EXCLUSIVE = true;// SystemProperties.getBoolean("ctq.applications.checkexclusive");

    static final boolean CHECK_EXHAUSTIVE = true; // SystemProperties.getBoolean("ctq.applications.checkexhaustive");

    static class Data {

        /**
         * Type qualifier annotations applied directly to
         * methods/fields/classes/etc.
         */
        private final Map<AnnotatedObject, Collection<AnnotationValue>> directObjectAnnotations = new HashMap<AnnotatedObject, Collection<AnnotationValue>>();

        /** Type qualifier annotations applied directly to method parameters. */
        private final HashMap<XMethod, Map<Integer, Collection<AnnotationValue>>> directParameterAnnotations = new HashMap<XMethod, Map<Integer, Collection<AnnotationValue>>>();

        /**
         * Map of TypeQualifierValues to maps containing, for each
         * AnnotatedObject, the effective TypeQualifierAnnotation (if any) for
         * that AnnotatedObject.
         */
        private final Map<TypeQualifierValue<?>, Map<AnnotatedObject, TypeQualifierAnnotation>> effectiveObjectAnnotations = new HashMap<TypeQualifierValue<?>, Map<AnnotatedObject, TypeQualifierAnnotation>>();

        /**
         * Map of TypeQualifierValues to maps containing, for each
         * XMethod/parameter, the effective TypeQualifierAnnotation (if any) for
         * that XMethod/parameter.
         */
        private final Map<TypeQualifierValue<?>, DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation>> effectiveParameterAnnotations = new HashMap<TypeQualifierValue<?>, DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation>>();
    }

    private static ThreadLocal<Data> instance = new ThreadLocal<Data>() {
        @Override
        protected Data initialValue() {
            if (DEBUG) {
                System.out.println("constructing TypeQualifierApplications.Data");
            }
            return new Data();
        }
    };

    public static void clearInstance() {
        instance.remove();
    }

    private static Map<TypeQualifierValue<?>, DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation>> getEffectiveParameterAnnotations() {
        return instance.get().effectiveParameterAnnotations;
    }

    private static Map<TypeQualifierValue<?>, Map<AnnotatedObject, TypeQualifierAnnotation>> getEffectiveObjectAnnotations() {
        return instance.get().effectiveObjectAnnotations;
    }

    private static HashMap<XMethod, Map<Integer, Collection<AnnotationValue>>> getDirectParameterAnnotations() {
        return instance.get().directParameterAnnotations;
    }

    private static Map<AnnotatedObject, Collection<AnnotationValue>> getDirectObjectAnnotations() {
        return instance.get().directObjectAnnotations;
    }

    public static void updateAnnotations(AnnotatedObject object) {
        // TODO: Be smarter. Can we do something other than clear everything?
        clearInstance();
    }

    /**
     * Callback interface to compute effective TypeQualifierAnnotation on an
     * AnnotatedObject or method parameter.
     */
    private interface ComputeEffectiveTypeQualifierAnnotation {
        public TypeQualifierAnnotation compute(TypeQualifierValue<?> tqv);
    }

    /**
     * Get the direct annotations (if any) on given AnnotatedObject.
     *
     * @param m
     *            an AnnotatedObject
     * @return Collection of AnnotationValues representing annotations directly
     *         applied to this AnnotatedObject
     */
    private static Collection<AnnotationValue> getDirectAnnotation(AnnotatedObject m) {
        Collection<AnnotationValue> result = getDirectObjectAnnotations().get(m);
        if (result != null) {
            return result;
        }
        if (m.getAnnotationDescriptors().isEmpty()) {
            return Collections.<AnnotationValue> emptyList();
        }
        result = TypeQualifierResolver.resolveTypeQualifiers(m.getAnnotations());
        if (result.size() == 0) {
            result = Collections.<AnnotationValue> emptyList();
        }
        getDirectObjectAnnotations().put(m, result);
        return result;
    }

    /**
     * Get the direct annotations (if any) on given method parameter.
     *
     * @param m
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @return Collection of AnnotationValues representing annotations directly
     *         applied to this parameter
     */
    private static Collection<AnnotationValue> getDirectAnnotation(XMethod m, int parameter) {
        HashMap<XMethod, Map<Integer, Collection<AnnotationValue>>> directParameterAnnotations = getDirectParameterAnnotations();
        Map<Integer, Collection<AnnotationValue>> map = directParameterAnnotations.get(m);
        if (map == null) {
            int n = m.getNumParams();
            if (m.isVarArgs())
            {
                n--; // ignore annotations on varargs parameters
            }
            map = new HashMap<Integer, Collection<AnnotationValue>>(n + 2);
            for (int i = 0; i < n; i++) {
                Collection<AnnotationValue> a = TypeQualifierResolver.resolveTypeQualifiers(m.getParameterAnnotations(i));
                if (!a.isEmpty()) {
                    map.put(i, a);
                }
            }
            if (map.isEmpty()) {
                map = Collections.emptyMap();
            }
            directParameterAnnotations.put(m, map);
        }

        Collection<AnnotationValue> result = map.get(parameter);
        if (result != null) {
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Populate a Set of TypeQualifierAnnotations representing directly-applied
     * type qualifier annotations on given method parameter.
     *
     * @param result
     *            Set of TypeQualifierAnnotations
     * @param o
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     */
    public static void getDirectApplications(Set<TypeQualifierAnnotation> result, XMethod o, int parameter) {
        Collection<AnnotationValue> values = getDirectAnnotation(o, parameter);
        for (AnnotationValue v : values) {
            constructTypeQualifierAnnotation(result, v);
        }

    }

    /**
     * Populate a Set of TypeQualifierAnnotations representing directly-applied
     * type qualifier annotations on given AnnotatedObject.
     *
     * @param result
     *            Set of TypeQualifierAnnotations
     * @param o
     *            an AnnotatedObject
     * @param e
     *            ElementType representing kind of annotated object
     */
    public static void getDirectApplications(Set<TypeQualifierAnnotation> result, AnnotatedObject o, ElementType e) {
        if (!o.getElementType().equals(e)) {
            return;
        }
        Collection<AnnotationValue> values = getDirectAnnotation(o);
        for (AnnotationValue v : values) {
            constructTypeQualifierAnnotation(result, v);
        }

    }

    /**
     * Resolve a raw AnnotationValue into a TypeQualifierAnnotation.
     *
     * @param v
     *            a raw AnnotationValue
     * @return a constructed TypeQualifierAnnotation
     */
    public static TypeQualifierAnnotation constructTypeQualifierAnnotation(AnnotationValue v) {
        assert v != null;
        EnumValue whenValue = (EnumValue) v.getValue("when");
        When when = whenValue == null ? When.ALWAYS : When.valueOf(whenValue.value);
        ClassDescriptor annotationClass = v.getAnnotationClass();
        TypeQualifierValue<?> tqv = TypeQualifierValue.getValue(annotationClass, v.getValue("value"));
        TypeQualifierAnnotation tqa = TypeQualifierAnnotation.getValue(tqv, when);
        return tqa;
    }

    /**
     * Resolve a raw AnnotationValue into a TypeQualifierAnnotation, storing
     * result in given Set.
     *
     * @param set
     *            Set of resolved TypeQualifierAnnotations
     * @param v
     *            a raw AnnotationValue
     */
    public static void constructTypeQualifierAnnotation(Set<TypeQualifierAnnotation> set, AnnotationValue v) {
        assert set != null;
        TypeQualifierAnnotation tqa = constructTypeQualifierAnnotation(v);
        set.add(tqa);
    }

    /**
     * Populate Set of TypeQualifierAnnotations for given AnnotatedObject,
     * taking into account annotations applied to outer scopes (e.g., enclosing
     * classes and packages.)
     *
     * @param result
     *            Set of TypeQualifierAnnotations
     * @param o
     *            an AnnotatedObject
     * @param e
     *            ElementType representing kind of AnnotatedObject
     */
    private static void getApplicableScopedApplications(Set<TypeQualifierAnnotation> result, AnnotatedObject o, ElementType e) {
        if (!o.isSynthetic()) {
            AnnotatedObject outer = o.getContainingScope();
            if (outer != null) {
                getApplicableScopedApplications(result, outer, e);
            }
        }
        getDirectApplications(result, o, e);
    }

    /**
     * Get the collection of resolved TypeQualifierAnnotations for a given
     * AnnotatedObject, taking into account annotations applied to outer scopes
     * (e.g., enclosing classes and packages.)
     *
     * @param o
     *            an AnnotatedObject
     * @param e
     *            ElementType representing kind of AnnotatedObject
     * @return Collection of resolved TypeQualifierAnnotations
     */
    private static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(AnnotatedObject o, ElementType e) {
        Set<TypeQualifierAnnotation> result = new HashSet<TypeQualifierAnnotation>();
        getApplicableScopedApplications(result, o, e);
        return result;
    }

    /**
     * Get the collection of resolved TypeQualifierAnnotations for a given
     * parameter, taking into account annotations applied to outer scopes (e.g.,
     * enclosing classes and packages.)
     *
     * @param o
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @return Collection of resolved TypeQualifierAnnotations
     */
    private static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(XMethod o, int parameter) {
        Set<TypeQualifierAnnotation> result = new HashSet<TypeQualifierAnnotation>();
        ElementType e = ElementType.PARAMETER;
        getApplicableScopedApplications(result, o, e);
        getDirectApplications(result, o, parameter);
        return result;
    }

    /**
     * Get the Collection of resolved TypeQualifierAnnotations representing
     * directly applied and default (outer scope) type qualifier annotations for
     * given AnnotatedObject.
     *
     * <p>
     * NOTE: does not properly account for inherited annotations on instance
     * methods. It is ok to call this method to find out generally-relevant
     * TypeQualifierAnnotations, but not to find the effective
     * TypeQualifierAnnotation.
     * </p>
     *
     * @param o
     *            an AnnotatedObject
     * @return Collection of TypeQualifierAnnotations applicable to the
     *         AnnotatedObject
     */
    public static Collection<TypeQualifierAnnotation> getApplicableApplications(AnnotatedObject o) {
        return getApplicableScopedApplications(o, o.getElementType());
    }

    /**
     * Get the Collection of resolved TypeQualifierAnnotations representing
     * directly applied and default (outer scope) type qualifier annotations for
     * given method parameter.
     *
     * <p>
     * NOTE: does not properly account for inherited annotations on instance
     * method parameters. It is ok to call this method to find out
     * generally-relevant TypeQualifierAnnotations, but not to find the
     * effective TypeQualifierAnnotation.
     * </p>
     *
     * @param o
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @return Collection of TypeQualifierAnnotations applicable to the method
     *         parameter
     */
    public static Collection<TypeQualifierAnnotation> getApplicableApplications(XMethod o, int parameter) {
        return getApplicableScopedApplications(o, parameter);
    }

    /**
     * Look up a TypeQualifierAnnotation matching given TypeQualifierValue.
     *
     * @param typeQualifierAnnotations
     *            a Collection of TypeQualifierAnnotations
     * @param typeQualifierValue
     *            a TypeQualifierValue
     * @return matching TypeQualifierAnnotation, or null if none
     */
    private static @CheckForNull
    TypeQualifierAnnotation findMatchingTypeQualifierAnnotation(Collection<TypeQualifierAnnotation> typeQualifierAnnotations,
            TypeQualifierValue<?> typeQualifierValue) {
        for (TypeQualifierAnnotation typeQualifierAnnotation : typeQualifierAnnotations) {
            if (typeQualifierAnnotation.typeQualifier.equals(typeQualifierValue)) {
                return typeQualifierAnnotation;
            }
        }
        return null;
    }

    /**
     * Look for a default type qualifier annotation.
     *
     * @param o
     *            an AnnotatedObject
     * @param typeQualifierValue
     *            a TypeQualifierValue
     * @param elementType
     *            type of element for which we're looking for a default
     *            annotation
     * @return default TypeQualifierAnnotation, or null if none
     */
    private static @CheckForNull
    TypeQualifierAnnotation getDefaultAnnotation(AnnotatedObject o, TypeQualifierValue<?> typeQualifierValue, ElementType elementType) {
        //
        // Try to find a default annotation using the standard JSR-305
        // default annotation mechanism.
        //
        TypeQualifierAnnotation result;
        Collection<AnnotationValue> values = TypeQualifierResolver.resolveTypeQualifierDefaults(o.getAnnotations(), elementType);
        TypeQualifierAnnotation tqa = extractAnnotation(values, typeQualifierValue);

        if (tqa != null) {
            // System.out.println("Found default annotation of " + tqa +
            // " for element " + elementType + " in " + o);
            return tqa;
        }

        //
        // Try one of the FindBugs-specific default annotation mechanisms.
        //

        if ((result = checkFindBugsDefaultAnnotation(FindBugsDefaultAnnotations.DEFAULT_ANNOTATION, o, typeQualifierValue)) != null) {
            return result;
        }

        switch (elementType) {
        case FIELD:
            result = checkFindBugsDefaultAnnotation(FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_FIELDS, o,
                    typeQualifierValue);
            break;
        case METHOD:
            result = checkFindBugsDefaultAnnotation(FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_METHODS, o,
                    typeQualifierValue);
            break;
        case PARAMETER:
            result = checkFindBugsDefaultAnnotation(FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_PARAMETERS, o,
                    typeQualifierValue);
            break;
        default:
            // ignore
            break;
        }

        // Try out default JDT (Eclipse) annotations
        if(result == null){
            AnnotationValue annotationValue = o.getAnnotation(TypeQualifierResolver.eclipseNonNullByDefault);
            if(annotationValue != null){
                Collection<AnnotationValue> resolvedTypeQualifiers = TypeQualifierResolver.resolveTypeQualifiers(annotationValue);
                tqa = extractAnnotation(resolvedTypeQualifiers, typeQualifierValue);
                if(tqa != null){
                    return tqa;
                }
            }
        }
        return result;
    }

    private static @CheckForNull
    TypeQualifierAnnotation checkFindBugsDefaultAnnotation(ClassDescriptor defaultAnnotation, AnnotatedObject o,
            TypeQualifierValue<?> typeQualifierValue) {

        if (DEBUG_DEFAULT_ANNOTATION) {
            System.out.println("Checking for " + defaultAnnotation + " containing " + typeQualifierValue + " on " + o);
        }
        // - check to see if default annotation is present; if not, return null
        AnnotationValue annotationValue = o.getAnnotation(defaultAnnotation);
        if (annotationValue == null) {
            if (DEBUG_DEFAULT_ANNOTATION) {
                System.out.println("   ===> no " + defaultAnnotation);
            }
            return null;
        }

        // - get value - should be Type or array of Type
        Object value = annotationValue.getValue("value");
        if (value == null) {
            if (DEBUG_DEFAULT_ANNOTATION) {
                System.out.println("   ===> value is null");
            }
            return null;
        }
        Object[] types;
        if (value instanceof Object[]) {
            types = (Object[]) value;
        } else {
            types = new Object[] { value };
        }

        // - scan through array elements; see if any match the
        // TypeQualifierValue (including type qualifier nicknames)
        for (Object obj : types) {
            if (!(obj instanceof Type)) {
                if (DEBUG_DEFAULT_ANNOTATION) {
                    System.out
                    .println("Found a non-Type value in value array of " + defaultAnnotation.toString() + " annotation");
                }
                continue;
            }

            Type type = (Type) obj;
            if (DEBUG_DEFAULT_ANNOTATION) {
                System.out.println("  ===> checking " + type.getDescriptor());
            }
            if (type.getDescriptor().startsWith("[")) {
                continue;
            }
            ClassDescriptor typeDesc = DescriptorFactory.instance().getClassDescriptor(type.getInternalName());

            // There is no general way to figure out whether a particular
            // type is a type qualifier we're interested in without
            // resolving it.
            AnnotationValue annotation = new AnnotationValue(typeDesc);
            Collection<AnnotationValue> resolvedTypeQualifiers = TypeQualifierResolver.resolveTypeQualifiers(annotation);
            TypeQualifierAnnotation tqa = extractAnnotation(resolvedTypeQualifiers, typeQualifierValue);
            if (tqa != null) {
                return tqa;
            }

        }

        return null;
    }

    private static TypeQualifierAnnotation extractAnnotation(Collection<AnnotationValue> resolvedTypeQualifiers,
            TypeQualifierValue<?> typeQualifierValue) {
        for (AnnotationValue typeQualifier : resolvedTypeQualifiers) {
            TypeQualifierAnnotation tqa = constructTypeQualifierAnnotation(typeQualifier);
            if (tqa.typeQualifier.equals(typeQualifierValue)) {
                if (DEBUG) {
                    System.out.println("  ===> Found match " + tqa);
                }
                return tqa;
            }
        }
        return null;
    }

    /**
     * Get the effective TypeQualifierAnnotation on given AnnotatedObject. Takes
     * into account inherited and default (outer scope) annotations. Also takes
     * exclusive qualifiers into account.
     *
     * @param o
     *            an AnnotatedObject
     * @param typeQualifierValue
     *            a TypeQualifierValue specifying kind of annotation we want to
     *            look up
     * @return the effective TypeQualifierAnnotation, or null if there is no
     *         effective TypeQualifierAnnotation on this AnnotatedObject
     */
    public static TypeQualifierAnnotation getEffectiveTypeQualifierAnnotation(AnnotatedObject o,
            TypeQualifierValue<?> typeQualifierValue) {
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
        TypeQualifierAnnotation tqa = computeEffectiveTypeQualifierAnnotation(typeQualifierValue, o);
        final AnnotatedObject o2 = o;
        if (CHECK_EXCLUSIVE && tqa == null && typeQualifierValue.isExclusiveQualifier()) {
            tqa = computeExclusiveQualifier(typeQualifierValue, new ComputeEffectiveTypeQualifierAnnotation() {
                @Override
                public TypeQualifierAnnotation compute(TypeQualifierValue<?> tqv) {
                    return computeEffectiveTypeQualifierAnnotation(tqv, o2);
                }

                @Override
                public String toString() {
                    return o2.toString();
                }
            });
        }

        return tqa;
    }

    private static TypeQualifierAnnotation computeEffectiveTypeQualifierAnnotation(TypeQualifierValue<?> typeQualifierValue,
            AnnotatedObject o) {

        Map<AnnotatedObject, TypeQualifierAnnotation> map = getEffectiveObjectAnnotations().get(typeQualifierValue);
        if (map == null) {
            map = new HashMap<AnnotatedObject, TypeQualifierAnnotation>();
            getEffectiveObjectAnnotations().put(typeQualifierValue, map);
        }

        // Check cached answer
        TypeQualifierAnnotation result;

        if (map.containsKey(o)) {
            result = map.get(o);
        } else {
            if (DEBUG) {
                System.out.println("Looking up application of " + typeQualifierValue + " on " + o);
            }

            // Compute answer
            TypeQualifierAnnotation tqa;

            // See if there is a direct application
            tqa = getDirectTypeQualifierAnnotation(o, typeQualifierValue);

            // If it's an instance method, check for an inherited annotation
            if (tqa == null && (o instanceof XMethod) && !((XMethod) o).isStatic() && !((XMethod) o).isPrivate()
                    && !"<init>".equals(((XMethod) o).getName())) {
                tqa = getInheritedTypeQualifierAnnotation((XMethod) o, typeQualifierValue);
            }

            boolean methodOverrides = false;
            if (tqa == TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION) {
                methodOverrides = true;
                tqa = null;
            }
            // Check for a default (outer scope) annotation
            if (tqa == null) {
                tqa = getDefaultTypeQualifierAnnotation(o, typeQualifierValue, methodOverrides);
            }

            // Cache computed answer
            result = tqa;
            map.put(o, result);
            if (DEBUG && result != null) {
                System.out.println("  => Answer: " + result.when + " on " + o);
            }

        }

        // Return cached answer
        return result;
    }

    /**
     * Get a directly-applied TypeQualifierAnnotation on given AnnotatedObject.
     *
     * @param o
     *            an AnnotatedObject
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return directly-applied TypeQualifierAnnotation, or null if there is no
     *         such annotation on the AnnotatedObject
     */
    private static TypeQualifierAnnotation getDirectTypeQualifierAnnotation(AnnotatedObject o,
            TypeQualifierValue<?> typeQualifierValue) {
        TypeQualifierAnnotation result;

        Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
        getDirectApplications(applications, o, o.getElementType());

        result = findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);

        return result;
    }

    /**
     * Get the effective inherited TypeQualifierAnnotation on given instance
     * method.
     *
     * @param o
     *            an XMethod
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return effective TypeQualifierAnnotation inherited from overridden
     *         supertype methods, or null if there is no inherited
     *         TypeQualifierAnnotation
     */
    public static TypeQualifierAnnotation getInheritedTypeQualifierAnnotation(XMethod o, TypeQualifierValue<?> typeQualifierValue) {
        assert !o.isStatic();

        ReturnTypeAnnotationAccumulator accumulator = new ReturnTypeAnnotationAccumulator(typeQualifierValue, o);
        try {
            AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypesDepthFirst(o.getClassDescriptor(), accumulator);
            TypeQualifierAnnotation result = accumulator.getResult().getEffectiveTypeQualifierAnnotation();
            if (result == null && accumulator.overrides()) {
                return TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION;
            }
            return result;
        } catch (ClassNotFoundException e) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
            return null;
        }
    }

    /**
     * Get the default (outer scope) annotation applicable to given
     * AnnotatedObject.
     *
     * @param o
     *            an AnnotatedObject
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return the applicable default TypeQualifierAnnotation, or null if there
     *         is no default TypeQualifierAnnotation
     */
    private static TypeQualifierAnnotation getDefaultTypeQualifierAnnotation(AnnotatedObject o,
            TypeQualifierValue<?> typeQualifierValue, boolean stopAtClassScope) {

        if (o.isSynthetic())
        {
            return null; // synthetic objects don't get default annotations
        }

        ElementType elementType = o.getElementType();
        while (true) {
            o = o.getContainingScope();
            if (o == null) {
                return null;
            }
            if (stopAtClassScope && o instanceof XClass) {
                return null;
            }
            TypeQualifierAnnotation result;

            // Check direct applications of the type qualifier
            Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
            getDirectApplications(applications, o, elementType);
            result = findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
            if (result != null) {
                // Great - found an outer scope with a relevant annotation
                assert false : "I don't think we should be looking here";
            return result;
            }

            // Check default annotations
            result = getDefaultAnnotation(o, typeQualifierValue, elementType);
            if (result != null) {
                return result;
            }
        }
    }

    /**
     * Get the effective TypeQualifierAnnotation on given method parameter.
     * Takes into account inherited and default (outer scope) annotations. Also
     * takes exclusive qualifiers into account.
     *
     * @param xmethod
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return effective TypeQualifierAnnotation on the parameter, or null if
     *         there is no effective TypeQualifierAnnotation
     */
    public static @CheckForNull
    TypeQualifierAnnotation getEffectiveTypeQualifierAnnotation(final XMethod xmethod, final int parameter,
            TypeQualifierValue<?> typeQualifierValue) {

        TypeQualifierAnnotation tqa = computeEffectiveTypeQualifierAnnotation(typeQualifierValue, xmethod, parameter);

        if (CHECK_EXCLUSIVE && tqa == null && typeQualifierValue.isExclusiveQualifier()) {
            tqa = computeExclusiveQualifier(typeQualifierValue, new ComputeEffectiveTypeQualifierAnnotation() {
                @Override
                public TypeQualifierAnnotation compute(TypeQualifierValue<?> tqv) {
                    return computeEffectiveTypeQualifierAnnotation(tqv, xmethod, parameter);
                }

                @Override
                public String toString() {
                    return "parameter " + parameter + " of " + xmethod;
                }
            });
        }

        return tqa;
    }

    // static Map<String, Throwable> checked = new HashMap<String, Throwable>();

    private static TypeQualifierAnnotation computeEffectiveTypeQualifierAnnotation(TypeQualifierValue<?> typeQualifierValue,
            XMethod xmethod, int parameter) {
        if (DEBUG) {
            // System.out.println("XX: "
            // +System.identityHashCode(typeQualifierValue));
            if (typeQualifierValue.value != null) {
                System.out.println("  Value is " + typeQualifierValue.value + "("
                        + typeQualifierValue.value.getClass().toString() + ")");
            }
        }
        Map<TypeQualifierValue<?>, DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation>> effectiveParameterAnnotations = getEffectiveParameterAnnotations();
        DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation> map = effectiveParameterAnnotations.get(typeQualifierValue);
        if (map == null) {
            if (DEBUG) {
                System.out.println("computeEffectiveTypeQualifierAnnotation: Creating map for " + typeQualifierValue);
            }
            map = new DualKeyHashMap<XMethod, Integer, TypeQualifierAnnotation>();
            effectiveParameterAnnotations.put(typeQualifierValue, map);
        }

        // Check cached answer
        TypeQualifierAnnotation result;
        if (map.containsKey(xmethod, parameter)) {
            result = map.get(xmethod, parameter);
        } else {
            if (DEBUG) {
                System.out.println("Looking up application of " + typeQualifierValue + " on " + xmethod + " parameter "
                        + parameter);
            }

            // String desc =
            // xmethod.toString()+":"+parameter+":"+typeQualifierValue;
            // if (checked.containsKey(desc)) {
            // //throw new IllegalStateException("Repeating computation of " +
            // desc, checked.get(desc));
            // System.out.println("Repeating computation of " + desc);
            // System.out.println("Previously computed:");
            // checked.get(desc).printStackTrace(System.out);
            // throw new IllegalStateException();
            // }
            // checked.put(desc, new Throwable().fillInStackTrace());

            // Compute answer
            TypeQualifierAnnotation tqa;

            if (xmethod.isVarArgs() && parameter == xmethod.getNumParams()-1) {
                tqa = null;
                if (DEBUG) {
                    System.out.print("  vararg parameters don't get type qualifiers");
                }
            }
            else {
                // Check direct application
                if (DEBUG) {
                    System.out.print("  (1) Checking direct application...");
                }
                tqa = getDirectTypeQualifierAnnotation(xmethod, parameter, typeQualifierValue);
                if (DEBUG) {
                    System.out.println(tqa != null ? "FOUND" : "none");
                }

                // If it's an instance method, check for inherited annotation
                if (tqa == null && !xmethod.isStatic() && !xmethod.isPrivate() && !"<init>".equals(xmethod.getName())) {
                    if (DEBUG) {
                        System.out.print("  (2) Checking inherited...");
                    }
                    tqa = getInheritedTypeQualifierAnnotation(xmethod, parameter, typeQualifierValue);
                    if (DEBUG) {
                        if (tqa == TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION) {
                            System.out.println("Overrides, no annotation inherited");
                        } else if (tqa != null) {
                            System.out.println("Inherited " + tqa.when);
                        } else {
                            System.out.println("Nothing inherited");
                        }
                    }
                }
                boolean overriddenMethod = false;
                if (tqa == TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION) {
                    overriddenMethod = true;
                    tqa = null;
                }
                // Check for default (outer scope) annotation
                if (tqa == null) {
                    if (xmethod.isVariableSynthetic((xmethod.isStatic() ? 0 : 1) + parameter)) {
                        if (DEBUG) {
                            System.out.print("  (3) Skipping default for synthetic parameter");
                        }
                    } else {
                        if (DEBUG) {
                            System.out.print("  (3) Checking default...");
                        }

                        tqa = getDefaultTypeQualifierAnnotationForParameters(xmethod, typeQualifierValue, overriddenMethod);

                        if (DEBUG) {
                            System.out.println(tqa != null ? "FOUND" : "none");
                        }
                    }
                }
            }

            // Cache answer
            result = tqa;
            map.put(xmethod, parameter, result);

            if (DEBUG) {
                if (result == null) {
                    System.out.println("  => Answer: no annotation on parameter " + parameter + " of " + xmethod);
                } else {
                    System.out.println("  => Answer: " + result.when + " on parameter " + parameter + " of " + xmethod);
                }
            }
        }

        if (!map.containsKey(xmethod, parameter)) {
            throw new IllegalStateException("Did not populate cache?");
        }

        // Return cached answer
        return result;
    }

    /**
     * Get the TypeQualifierAnnotation directly applied to given method
     * parameter.
     *
     * @param xmethod
     *            a method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return TypeQualifierAnnotation directly applied to the parameter, or
     *         null if there is no directly applied TypeQualifierAnnotation
     */
    public static @CheckForNull @CheckReturnValue
    TypeQualifierAnnotation getDirectTypeQualifierAnnotation(XMethod xmethod, int parameter, TypeQualifierValue<?> typeQualifierValue) {
        XMethod bridge = xmethod.bridgeTo();
        if (bridge != null) {
            xmethod = bridge;
        }
        Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
        getDirectApplications(applications, xmethod, parameter);
        if (DEBUG_METHOD != null && DEBUG_METHOD.equals(xmethod.getName())) {
            System.out.println("  Direct applications are: " + applications);
        }

        return findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
    }

    /**
     * Get the effective inherited TypeQualifierAnnotation on the given instance
     * method parameter.
     *
     * @param xmethod
     *            an instance method
     * @param parameter
     *            a parameter (0 == first parameter)
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @return effective inherited TypeQualifierAnnotation on the parameter, or
     *         null if there is not effective TypeQualifierAnnotation
     */
    public static @CheckForNull
    TypeQualifierAnnotation getInheritedTypeQualifierAnnotation(XMethod xmethod, int parameter,
            TypeQualifierValue<?> typeQualifierValue) {
        assert !xmethod.isStatic();

        ParameterAnnotationAccumulator accumulator = new ParameterAnnotationAccumulator(typeQualifierValue, xmethod, parameter);
        try {
            AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypesDepthFirst(xmethod.getClassDescriptor(), accumulator);
            TypeQualifierAnnotation result = accumulator.getResult().getEffectiveTypeQualifierAnnotation();
            if (result == null && accumulator.overrides()) {
                return TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION;
            }
            return result;
        } catch (ClassNotFoundException e) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
            return null;
        }
    }

    /**
     * Get the default (outer-scope) TypeQualifierAnnotation on given method
     * parameter.
     *
     * @param xmethod
     *            a method
     * @param typeQualifierValue
     *            the kind of TypeQualifierValue we are looking for
     * @param stopAtMethodScope
     * @return the default (outer scope) TypeQualifierAnnotation on the
     *         parameter, or null if there is no default TypeQualifierAnnotation
     */
    private static @CheckForNull
    TypeQualifierAnnotation getDefaultTypeQualifierAnnotationForParameters(XMethod xmethod,
            TypeQualifierValue<?> typeQualifierValue, boolean stopAtMethodScope) {

        if (xmethod.isSynthetic())
        {
            return null; // synthetic methods don't get default annotations
        }
        // System.out.println("Looking for default " + typeQualifierValue +
        // " annotation of parameters of " + xmethod);
        if ("<init>".equals(xmethod.getName()) && xmethod.getClassDescriptor().isAnonymousClass())
        {
            return null; // constructors for anonymous inner classes don't get
            // default annotations
        }

        /** private methods don't inherit from class or package scope */
        if (xmethod.isPrivate()) {
            stopAtMethodScope = true;
        }

        boolean stopAtClassScope = false;

        if (!xmethod.isPublic() && !xmethod.isProtected() && (xmethod.isStatic() || "<init>".equals(xmethod.getName()))) {
            try {
                XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, xmethod.getClassDescriptor());
                stopAtClassScope = xclass.isPrivate();
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Problem resolving class for " + xmethod, e);
            }
        }

        AnnotatedObject o = xmethod;
        while (true) {
            if (o == null) {
                return null;
            }

            if (stopAtMethodScope && o instanceof XClass) {
                return null;
            }
            // Check for direct type qualifier annotation
            Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
            getDirectApplications(applications, o, ElementType.PARAMETER);
            TypeQualifierAnnotation tqa = findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
            if (tqa != null) {
                // Found matching annotation in outer scope
                assert false : "I think this code is dead; it shouldn't find anything";
            return tqa;
            }
            // Check for default annotation
            tqa = getDefaultAnnotation(o, typeQualifierValue, ElementType.PARAMETER);
            if (tqa != null) {
                if (DEBUG) {
                    System.out.println("Found default of " + tqa + " @ " + o);
                }
                return tqa;
            }
            if (stopAtClassScope && o instanceof XClass) {
                return null;
            }

            o = o.getContainingScope();

        }

    }

    private static TypeQualifierAnnotation computeExclusiveQualifier(TypeQualifierValue<?> typeQualifierValue,
            ComputeEffectiveTypeQualifierAnnotation c) {
        assert typeQualifierValue.isExclusiveQualifier();

        boolean isExhaustive = CHECK_EXHAUSTIVE && typeQualifierValue.isExhaustiveQualifier();

        // Exclusive qualifiers:
        // - if there is an effective application of
        // a "complementary" TypeQualifierValue in which
        // when=ALWAYS. If so, then it's effectively
        // the same as the asked-for TypeQualifierValue,
        // but with when=NEVER.
        //
        // Exhaustive qualifiers:
        // - if all effective applications of "complementary"
        // TypeQualifierValues
        // are when=NEVER, then the asked-for TypeQualifierValue
        // is effectively when=ALWAYS.

        boolean allComplementaryValuesAreWhenEqualsNever = true;

        Collection<TypeQualifierValue<?>> complementaryTypeQualifierValues = TypeQualifierValue
                .getComplementaryExclusiveTypeQualifierValue(typeQualifierValue);

        for (TypeQualifierValue<?> complementaryTypeQualifierValue : complementaryTypeQualifierValues) {
            TypeQualifierAnnotation complementaryTqa = c.compute(complementaryTypeQualifierValue);
            if (complementaryTqa != null) {
                if (complementaryTqa.when == When.ALWAYS) {
                    // Exclusive qualifier where a complementary qualifier
                    // was observed effectively when=ALWAYS.
                    return TypeQualifierAnnotation.getValue(typeQualifierValue, When.NEVER);
                } else if (complementaryTqa.when != When.NEVER) {
                    allComplementaryValuesAreWhenEqualsNever = false;
                }
            } else {
                allComplementaryValuesAreWhenEqualsNever = false;
            }
        }

        if (isExhaustive && allComplementaryValuesAreWhenEqualsNever) {
            // It's an exhaustive qualifier, and all complementary
            // qualifiers were effectively when=NEVER.
            if (TypeQualifierValue.DEBUG) {
                System.out.println("*** application of " + typeQualifierValue + " on " + c + " is when=ALWAYS due to exhaustion");
            }
            return TypeQualifierAnnotation.getValue(typeQualifierValue, When.ALWAYS);
        }

        return null;
    }
}
