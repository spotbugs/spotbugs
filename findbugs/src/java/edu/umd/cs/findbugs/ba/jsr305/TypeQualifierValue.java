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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.util.Util;

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

    private final @CheckForNull
    TypeQualifierValidator<A> validator;

    private final static ClassLoader validatorLoader = new ValidatorClassLoader();


    private TypeQualifierValue(ClassDescriptor typeQualifier, @CheckForNull Object value) {
        this.typeQualifier = typeQualifier;
        this.value = value;
        boolean isStrict = false; // will be set to true if this is a strict
                                  // type qualifier value
        boolean isExclusive = false; // will be set to true if this is an
                                     // exclusive type qualifier value
        boolean isExhaustive = false; // will be set to true if this is an
                                      // exhaustive type qualifier value

        TypeQualifierValidator<A> validator = null;
        Class<A> qualifierClass = null;
        A proxy = null;
        try {
            XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, typeQualifier);

            // Annotation elements appear as abstract methods in the annotation
            // class (interface).
            // So, if the type qualifier annotation has specified a default When
            // value,
            // it will appear as an abstract method called "when".
            XMethod whenMethod = xclass.findMethod("when", "()Ljavax/annotation/meta/When;", false);
            if (whenMethod == null) {
                isStrict = true;
            }
            for (XMethod xmethod : xclass.getXMethods()) {
                if (xmethod.getName().equals("value") && xmethod.getSignature().startsWith("()")) {
                    isExhaustive = xmethod.getAnnotation(EXHAUSTIVE_ANNOTATION) != null;
                    if (isExhaustive) {
                        // exhaustive qualifiers are automatically exclusive
                        isExclusive = true;
                    } else {
                        // see if there is an explicit @Exclusive annotation
                        isExclusive = xmethod.getAnnotation(EXCLUSIVE_ANNOTATION) != null;
                    }

                    break;
                }
            }
        } catch (MissingClassException e) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassNotFoundException());
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error looking up annotation class " + typeQualifier.toDottedClassName(), e);
        }
        this.isStrict = isStrict;
        this.isExclusive = isExclusive;
        this.isExhaustive = isExhaustive;
        ClassDescriptor checkerName = DescriptorFactory.createClassDescriptor(typeQualifier.getClassName() + "$Checker");
        try {
            Global.getAnalysisCache().getClassAnalysis(ClassData.class, checkerName);
            // found it.
//            System.out.println(checkerName);
            SecurityManager m = System.getSecurityManager();
            if (m == null)
                System.setSecurityManager(new ValidationSecurityManager());
            Class<?> c = validatorLoader.loadClass(checkerName.getDottedClassName());
             if (TypeQualifierValidator.class.isAssignableFrom(c)) {
                Class<? extends TypeQualifierValidator> checkerClass = c.asSubclass(TypeQualifierValidator.class);
                validator = getValidator(checkerClass);
                qualifierClass = getQualifierClass(typeQualifier);

                InvocationHandler handler = new InvocationHandler() {

                    public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
                       if (arg1.getName() == "value")
                           return TypeQualifierValue.this.value;
                       throw new UnsupportedOperationException("Can't handle " + arg1);
                    }};

                proxy =  qualifierClass.cast(Proxy.newProxyInstance(validatorLoader, new Class[] {qualifierClass}, handler));
            }
        } catch (ClassNotFoundException e) {
            assert true; // ignore
        } catch (CheckedAnalysisException e) {
            assert true; // ignore
        } catch (Exception e) {
            AnalysisContext.logError("Unable to construct type qualifier checker " + checkerName, e);
        } catch (Throwable e) {
            AnalysisContext.logError("Unable to construct type qualifier checker " + checkerName + " due to " + e.getClass().getSimpleName() + ":" + e.getMessage());
        }
        this.validator = validator;
        this.typeQualifierClass = qualifierClass;
        this.proxy = proxy;
    }

    /**
     * @param checkerClass
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    private TypeQualifierValidator<A> getValidator(Class<? extends TypeQualifierValidator> checkerClass)
            throws InstantiationException, IllegalAccessException {
        return checkerClass.newInstance();
    }

    /**
     * @param typeQualifier
     * @return
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private Class<A> getQualifierClass(ClassDescriptor typeQualifier) throws ClassNotFoundException {
        return (Class<A>) validatorLoader.loadClass(typeQualifier.getDottedClassName());
    }

    static class Data {
        /**
         * Cache in which constructed TypeQualifierValues are interned.
         */
        DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue<?>> typeQualifierMap = new DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue<?>>();

        /**
         * Set of all known TypeQualifierValues.
         */
        Set<TypeQualifierValue<?>> allKnownTypeQualifiers = new HashSet<TypeQualifierValue<?>>();
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

    public boolean canValidate(Object constantValue) {
        if (validator == null)
            return false;
        return true;
    }

    private static final InheritableThreadLocal<AtomicBoolean> performingValidation
        = new InheritableThreadLocal<AtomicBoolean>() {
        @Override protected AtomicBoolean initialValue() {
            return new AtomicBoolean();
        }

    };

    static final class ValidationSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
//            System.out.println("Checking " + perm);
            if (performingValidation.get().get())
                throw new SecurityException("not permissions granted while performing JSR-305 validation");
        }
        @Override
        public void checkPermission(Permission perm, Object context) {
            if (performingValidation.get().get())
                throw new SecurityException("not permissions granted while performing JSR-305 validation");
        }
    }


    public When validate(Object constantValue) {
        if (validator == null)
            throw new IllegalStateException("No validator");
        IAnalysisCache analysisCache = Global.getAnalysisCache();
        Profiler profiler = analysisCache.getProfiler();
        profiler.start(validator.getClass());
        AtomicBoolean performing = performingValidation.get();
        try {
            if (!performing.compareAndSet(false, true)) {
                throw new IllegalStateException("recursive validation");
            }

            return validator.forConstantValue(proxy, constantValue);
        } catch (Exception e) {
            AnalysisContext.logError("Error executing custom validator for " + typeQualifier + " " + constantValue, e);
            return When.UNKNOWN;
        } finally {
            if (!performing.compareAndSet(true, false)) {
                throw new IllegalStateException("performingValidation not set when validation completes");
            }
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
    public static @Nonnull
    TypeQualifierValue<?> getValue(ClassDescriptor desc, Object value) {
        DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue<?>> map = instance.get().typeQualifierMap;
        TypeQualifierValue<?> result = map.get(desc, value);
        if (result != null)
            return result;
        result = new TypeQualifierValue(desc, value);
        map.put(desc, value, result);
        instance.get().allKnownTypeQualifiers.add(result);
        return result;
    }
    @SuppressWarnings("unchecked")
    public static @Nonnull <A extends Annotation>
    TypeQualifierValue<A> getValue(Class <A> clazz, Object value) {
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
    public static Collection<TypeQualifierValue> getComplementaryExclusiveTypeQualifierValue(TypeQualifierValue tqv) {
        assert tqv.isExclusiveQualifier();

        LinkedList<TypeQualifierValue> result = new LinkedList<TypeQualifierValue>();

        for (TypeQualifierValue t : instance.get().allKnownTypeQualifiers) {
            //
            // Any TypeQualifierValue with the same
            // annotation class but a different value is a complementary
            // type qualifier.
            //
            if (t.typeQualifier.equals(tqv.typeQualifier) && !Util.nullSafeEquals(t.value, tqv.value)) {
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
    public static boolean hasMultipleVariants(TypeQualifierValue tqv) {
        int count = 0;
        for (TypeQualifierValue t : instance.get().allKnownTypeQualifiers) {
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
        if (value != null)
            result += 37 * value.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeQualifierValue))
            return false;
        TypeQualifierValue other = (TypeQualifierValue) o;
        return typeQualifier.equals(other.typeQualifier) && Util.nullSafeEquals(value, other.value);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
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
