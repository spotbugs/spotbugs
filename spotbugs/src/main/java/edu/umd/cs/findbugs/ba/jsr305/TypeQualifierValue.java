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

package edu.umd.cs.findbugs.ba.jsr305;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * A TypeQualifierValue is a pair specifying a type qualifier annotation and a
 * value. Each TypeQualifierValue is effectively a different type qualifier. For
 * example, if Foo is a type qualifier annotation having an int value, then
 * Foo(0), Foo(1), etc. are all different type qualifiers which must be checked
 * separately.
 *
 * @author William Pugh
 */
public class TypeQualifierValue<A extends Annotation> {
    public static final boolean DEBUG = SystemProperties.getBoolean("tqv.debug");
    public static final boolean DEBUG_CLASSLOADING = SystemProperties.getBoolean("tqv.debug.classloading");

    private static final ClassDescriptor EXCLUSIVE_ANNOTATION = DescriptorFactory.instance().getClassDescriptor(
            javax.annotation.meta.Exclusive.class);

    private static final ClassDescriptor EXHAUSTIVE_ANNOTATION = DescriptorFactory.instance().getClassDescriptor(
            javax.annotation.meta.Exhaustive.class);

    public final ClassDescriptor typeQualifier;
    public final Class<A> typeQualifierClass;

    public final A proxy;

    public final @CheckForNull Object value;

    private final boolean isStrict;

    private final boolean isExclusive;

    private final boolean isExhaustive;

    private final @CheckForNull TypeQualifierValidator<A> validator;



    private TypeQualifierValue(ClassDescriptor typeQualifier, @CheckForNull Object value) {
        this.typeQualifier = typeQualifier;
        this.value = value;
        /**  will be set to true if this is a strict type qualifier value */
        boolean isStrict1 = false;
        /**  will be set to true if this is an exclusive type qualifier value */
        boolean isExclusive1 = false;
        /** will be set to true if this is an exhaustive type qualifier value */
        boolean isExhaustive1 = false;
        TypeQualifierValidator<A> validator1 = null;
        Class<A> qualifierClass = null;
        XClass xclass = null;
        A proxy1 = null;
        try {
            xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, typeQualifier);

            // Annotation elements appear as abstract methods in the annotation
            // class (interface).
            // So, if the type qualifier annotation has specified a default When
            // value,
            // it will appear as an abstract method called "when".
            XMethod whenMethod = xclass.findMethod("when", "()Ljavax/annotation/meta/When;", false);
            if (whenMethod == null) {
                isStrict1 = true;
            }
            for (XMethod xmethod : xclass.getXMethods()) {
                if ("value".equals(xmethod.getName()) && xmethod.getSignature().startsWith("()")) {
                    isExhaustive1 = xmethod.getAnnotation(EXHAUSTIVE_ANNOTATION) != null;
                    if (isExhaustive1) {
                        // exhaustive qualifiers are automatically exclusive
                        isExclusive1 = true;
                    } else {
                        // see if there is an explicit @Exclusive annotation
                        isExclusive1 = xmethod.getAnnotation(EXCLUSIVE_ANNOTATION) != null;
                    }

                    break;
                }
            }
        } catch (MissingClassException e) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassNotFoundException());
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error looking up annotation class " + typeQualifier.toDottedClassName(), e);
        }
        this.isStrict = isStrict1;
        this.isExclusive = isExclusive1;
        this.isExhaustive = isExhaustive1;


        if (xclass != null) {
            ClassDescriptor checkerName = DescriptorFactory.createClassDescriptor(typeQualifier.getClassName() + "$Checker");

            if (!SystemProperties.RUNNING_AS_IDE_PLUGIN) {
                /* don't do this if running in Eclipse; check below is the quick
                   fix for bug 3599258 (Random obscure Eclipse failures during analysis)
                
                   Also don't do this if running in IntelliJ IDEA. This causes weird issues
                   either (see IDEA-230268)
                 */

                try {
                    Global.getAnalysisCache().getClassAnalysis(ClassData.class, checkerName);

                    Class<?> c = ValidatorClassLoader.INSTANCE.loadClass(checkerName.getDottedClassName());
                    if (TypeQualifierValidator.class.isAssignableFrom(c)) {

                        @SuppressWarnings("unchecked")
                        Class<? extends TypeQualifierValidator<A>> validatorClass = (Class<? extends TypeQualifierValidator<A>>) c
                                .asSubclass(TypeQualifierValidator.class);
                        validator1 = getValidator(validatorClass);
                        qualifierClass = getQualifierClass(typeQualifier);

                        InvocationHandler handler = (arg0, arg1, arg2) -> {
                            if ("value".equals(arg1.getName())) {
                                return TypeQualifierValue.this.value;
                            }
                            throw new UnsupportedOperationException("Can't handle " + arg1);
                        };

                        proxy1 = qualifierClass.cast(Proxy.newProxyInstance(ValidatorClassLoader.INSTANCE,
                                new Class[] { qualifierClass }, handler));
                    }

                } catch (ClassNotFoundException e) {
                    assert true; // ignore
                } catch (CheckedAnalysisException e) {
                    assert true; // ignore
                } catch (Exception e) {
                    AnalysisContext.logError("Unable to construct type qualifier checker " + checkerName, e);
                } catch (Throwable e) {
                    AnalysisContext.logError("Unable to construct type qualifier checker " + checkerName + " due to "
                            + e.getClass().getSimpleName() + ":" + e.getMessage());
                }
            }
        }
        this.validator = validator1;
        this.typeQualifierClass = qualifierClass;
        this.proxy = proxy1;
    }

    private static <A extends Annotation> TypeQualifierValidator<A> getValidator(
            Class<? extends TypeQualifierValidator<A>> checkerClass)
            throws InstantiationException, IllegalAccessException {
        return checkerClass.newInstance();
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> getQualifierClass(ClassDescriptor typeQualifier) throws ClassNotFoundException {
        @DottedClassName
        String className = typeQualifier.getDottedClassName();
        if (DEBUG_CLASSLOADING) {
            System.out.println("Getting qualifier class for " + className);
        }
        if (className.startsWith("javax.annotation") || className.startsWith("jakarta.annotation")) {
            return (Class<A>) Class.forName(className);
        }
        try {
            Global.getAnalysisCache().getClassAnalysis(ClassData.class, typeQualifier);
        } catch (CheckedAnalysisException e) {
            throw new ClassNotFoundException("No class data found for " + className);
        }

        ValidatorClassLoader validatorLoader = ValidatorClassLoader.INSTANCE;
        return (Class<A>) validatorLoader.loadClass(typeQualifier.getDottedClassName());
    }

    static byte[] loadClassData(String name) throws CheckedAnalysisException {
        ClassDescriptor d = DescriptorFactory.createClassDescriptorFromDottedClassName(name);
        ClassData data = Global.getAnalysisCache().getClassAnalysis(ClassData.class, d);
        return data.getData();
    }

    static class Data {
        /**
         * Cache in which constructed TypeQualifierValues are interned.
         */
        DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue<?>> typeQualifierMap = new DualKeyHashMap<>();

        /**
         * Set of all known TypeQualifierValues.
         */
        Set<TypeQualifierValue<?>> allKnownTypeQualifiers = new HashSet<>();
    }

    private static ThreadLocal<Data> instance = new ThreadLocal<Data>() {
        @Override
        protected Data initialValue() {
            return new Data();
        }
    };

    public static void clearInstance() {
        instance.remove();
    }

    public boolean canValidate(@CheckForNull Object constantValue) {
        return validator != null;
    }

    public When validate(@CheckForNull Object constantValue) {
        if (validator == null) {
            throw new IllegalStateException("No validator");
        }
        IAnalysisCache analysisCache = Global.getAnalysisCache();
        Profiler profiler = analysisCache.getProfiler();
        profiler.start(validator.getClass());
        try {
            return validator.forConstantValue(proxy, constantValue);
        } catch (Exception e) {
            AnalysisContext.logError("Error executing custom validator for " + typeQualifier + " " + constantValue, e);
            return When.UNKNOWN;
        } finally {
            profiler.end(validator.getClass());
        }
    }

    /**
     * Given a ClassDescriptor/value pair, return the interned
     * TypeQualifierValue representing that pair.
     *
     * @param desc
     *            a ClassDescriptor denoting a type qualifier annotation
     * @param value
     *            a value
     * @return an interned TypeQualifierValue object
     */
    @SuppressWarnings("rawtypes")
    public static @Nonnull TypeQualifierValue<?> getValue(ClassDescriptor desc, @CheckForNull Object value) {
        DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue<?>> map = instance.get().typeQualifierMap;
        TypeQualifierValue<?> result = map.get(desc, value);
        if (result != null) {
            return result;
        }
        result = new TypeQualifierValue(desc, value);
        map.put(desc, value, result);
        instance.get().allKnownTypeQualifiers.add(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static @Nonnull <A extends Annotation> TypeQualifierValue<A> getValue(Class<A> clazz, @CheckForNull Object value) {
        return (TypeQualifierValue<A>) getValue(DescriptorFactory.createClassDescriptor(clazz), value);
    }

    /**
     * Get Collection of all known TypeQualifierValues.
     *
     * @return Collection of all known TypeQualifierValues
     */
    public static Collection<TypeQualifierValue<?>> getAllKnownTypeQualifiers() {
        return Collections.unmodifiableSet(instance.get().allKnownTypeQualifiers);
    }

    /**
     * Get the "complementary" TypeQualifierValues for given exclusive type
     * qualifier.
     *
     * @param tqv
     *            a type qualifier (which must be exclusive)
     * @return Collection of complementary exclusive type qualifiers
     */
    public static Collection<TypeQualifierValue<?>> getComplementaryExclusiveTypeQualifierValue(TypeQualifierValue<?> tqv) {
        assert tqv.isExclusiveQualifier();

        LinkedList<TypeQualifierValue<?>> result = new LinkedList<>();

        for (TypeQualifierValue<?> t : instance.get().allKnownTypeQualifiers) {
            //
            // Any TypeQualifierValue with the same
            // annotation class but a different value is a complementary
            // type qualifier.
            //
            if (t.typeQualifier.equals(tqv.typeQualifier) && !Objects.equals(t.value, tqv.value)) {
                result.add(t);
            }
        }

        return result;
    }

    /**
     * Determine whether or not given TypeQualifierValue has multiple variants.
     * I.e., if Color is a type qualifier having values RED, GREEN, and BLUE,
     * then there are 3 variants, Color(RED), Color(GREEN), and COLOR(BLUE).
     *
     * @param tqv
     *            a TypeQualifierValue
     * @return true if there are multiple variants of this type qualifier, false
     *         otherwise
     */
    public static boolean hasMultipleVariants(TypeQualifierValue<?> tqv) {
        int count = 0;
        for (TypeQualifierValue<?> t : instance.get().allKnownTypeQualifiers) {
            if (t.typeQualifier.equals(tqv.typeQualifier)) {
                count++;
            }
        }
        return count > 1;
    }

    /**
     * Get the ClassDescriptor which specifies the type qualifier annotation.
     *
     * @return ClassDescriptor which specifies the type qualifier annotation
     */
    public ClassDescriptor getTypeQualifierClassDescriptor() {
        return typeQualifier;
    }

    /**
     * Return whether or not this TypeQualifierValue denotes a strict qualifier.
     *
     * @return true if type qualifier is strict, false otherwise
     */
    public boolean isStrictQualifier() {
        return isStrict;
    }

    /**
     * Return whether or not this TypeQualifierValue denotes an exclusive
     * qualifier.
     *
     * @return true if type qualifier is exclusive, false otherwise
     */
    public boolean isExclusiveQualifier() {
        return isExclusive;
    }

    /**
     * Return whether or not this TypeQualifierValue denotes an exhaustive
     * qualifier.
     *
     * @return true if type qualifier is exhaustive, false otherwise
     */
    public boolean isExhaustiveQualifier() {
        return isExhaustive;
    }

    @Override
    public int hashCode() {
        int result = typeQualifier.hashCode();
        if (value != null) {
            result += 37 * value.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeQualifierValue)) {
            return false;
        }
        TypeQualifierValue<?> other = (TypeQualifierValue<?>) o;
        return typeQualifier.equals(other.typeQualifier) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(typeQualifier.toString());
        if (value != null) {
            buf.append(':');
            buf.append(value.toString());
        }
        return buf.toString();
    }

}
