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

package edu.umd.cs.findbugs.ba.jsr305;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;

/**
 * Resolve annotations into type qualifiers.
 *
 * @author William Pugh
 */
public class TypeQualifierResolver {
    static final ClassDescriptor typeQualifier = DescriptorFactory.createClassDescriptor(javax.annotation.meta.TypeQualifier.class);

    static final ClassDescriptor typeQualifierNickname = DescriptorFactory
            .createClassDescriptor(javax.annotation.meta.TypeQualifierNickname.class);

    static final ClassDescriptor typeQualifierDefault = DescriptorFactory
            .createClassDescriptor(javax.annotation.meta.TypeQualifierDefault.class);

    static final ClassDescriptor elementTypeDescriptor = DescriptorFactory
            .createClassDescriptor(ElementType.class);

    static final ClassDescriptor androidNullable = DescriptorFactory.createClassDescriptor("android/support/annotation/Nullable");

    static final ClassDescriptor androidNonNull = DescriptorFactory.createClassDescriptor("android/support/annotation/NonNull");

    static final ClassDescriptor androidxNullable = DescriptorFactory.createClassDescriptor("androidx/annotation/Nullable");

    static final ClassDescriptor androidxNonNull = DescriptorFactory.createClassDescriptor("androidx/annotation/NonNull");

    static final ClassDescriptor googleNullable = DescriptorFactory.createClassDescriptor("com/google/common/base/Nullable");

    static final ClassDescriptor avroNullable = DescriptorFactory.createClassDescriptor("org/apache/avro/reflect/Nullable");

    static final ClassDescriptor intellijNullable = DescriptorFactory.createClassDescriptor("org/jetbrains/annotations/Nullable");

    static final ClassDescriptor intellijNotNull = DescriptorFactory.createClassDescriptor("org/jetbrains/annotations/NotNull");

    static final ClassDescriptor eclipseNullable = DescriptorFactory.createClassDescriptor("org/eclipse/jdt/annotation/Nullable");

    static final ClassDescriptor eclipseNonNull = DescriptorFactory.createClassDescriptor("org/eclipse/jdt/annotation/NonNull");

    static final ClassDescriptor checkerFrameworkNullable = DescriptorFactory.createClassDescriptor(
            "org/checkerframework/checker/nullness/qual/Nullable");

    static final ClassDescriptor checkerFrameworkNullableDecl = DescriptorFactory.createClassDescriptor(
            "org/checkerframework/checker/nullness/compatqual/NullableDecl");

    static final ClassDescriptor checkerFrameworkNonNull = DescriptorFactory.createClassDescriptor(
            "org/checkerframework/checker/nullness/qual/NonNull");

    static final ClassDescriptor checkerFrameworkNonNullDecl = DescriptorFactory.createClassDescriptor(
            "org/checkerframework/checker/nullness/compatqual/NonNullDecl");

    // javax.annotations.ParametersAreNonnullByDefault ?
    static final ClassDescriptor eclipseNonNullByDefault = DescriptorFactory.createClassDescriptor("org/eclipse/jdt/annotation/NonNullByDefault");

    /**
     * Resolve an AnnotationValue into a list of AnnotationValues representing
     * type qualifier annotations.
     *
     * @param value
     *            AnnotationValue representing the use of an annotation
     * @return Collection of AnnotationValues representing resolved
     *         TypeQualifier annotations
     */
    public static Collection<AnnotationValue> resolveTypeQualifiers(AnnotationValue value) {
        LinkedList<AnnotationValue> result = new LinkedList<>();
        resolveTypeQualifierNicknames(value, result, new LinkedList<ClassDescriptor>());
        return result;
    }

    /**
     * Resolve collection of AnnotationValues (which have been used to annotate
     * an AnnotatedObject or method parameter) into collection of resolved type
     * qualifier AnnotationValues.
     *
     * @param values
     *            Collection of AnnotationValues used to annotate an
     *            AnnotatedObject or method parameter
     * @return Collection of resolved type qualifier AnnotationValues
     */
    public static Collection<AnnotationValue> resolveTypeQualifierDefaults(Collection<AnnotationValue> values,
            ElementType elementType) {
        LinkedList<AnnotationValue> result = new LinkedList<>();
        for (AnnotationValue value : values) {
            resolveTypeQualifierDefaults(value, elementType, result);
        }
        return result;
    }

    /**
     * Resolve an annotation into AnnotationValues representing any type
     * qualifier(s) the annotation resolves to. Detects annotations which are
     * directly marked as TypeQualifier annotations, and also resolves the use
     * of TypeQualifierNickname annotations.
     *
     * @param value
     *            AnnotationValue representing the use of an annotation
     * @param result
     *            LinkedList containing resolved type qualifier AnnotationValues
     * @param onStack
     *            stack of annotations being processed; used to detect cycles in
     *            type qualifier nicknames
     */
    private static void resolveTypeQualifierNicknames(AnnotationValue value, LinkedList<AnnotationValue> result,
            LinkedList<ClassDescriptor> onStack) {
        ClassDescriptor annotationClass = value.getAnnotationClass();

        if (onStack.contains(annotationClass)) {
            AnalysisContext.logError("Cycle found in type nicknames: " + onStack);
            return;
        }
        try {
            onStack.add(annotationClass);

            try {
                if (annotationClass.equals(androidNullable)
                        || annotationClass.equals(androidxNullable)
                        || annotationClass.equals(googleNullable)
                        || annotationClass.equals(avroNullable)
                        || annotationClass.equals(eclipseNullable)
                        || annotationClass.equals(intellijNullable)
                        || annotationClass.equals(checkerFrameworkNullable)
                        || annotationClass.equals(checkerFrameworkNullableDecl)) {
                    resolveTypeQualifierNicknames(new AnnotationValue(JSR305NullnessAnnotations.CHECK_FOR_NULL), result, onStack);
                    return;
                }
                if (annotationClass.equals(androidNonNull)
                        || annotationClass.equals(androidxNonNull)
                        || annotationClass.equals(eclipseNonNull)
                        || annotationClass.equals(eclipseNonNullByDefault)
                        || annotationClass.equals(intellijNotNull)
                        || annotationClass.equals(checkerFrameworkNonNull)
                        || annotationClass.equals(checkerFrameworkNonNullDecl)) {
                    resolveTypeQualifierNicknames(new AnnotationValue(JSR305NullnessAnnotations.NONNULL), result, onStack);
                    return;
                }

                XClass c = Global.getAnalysisCache().getClassAnalysis(XClass.class, annotationClass);
                if (c.getAnnotationDescriptors().contains(typeQualifierNickname)) {
                    for (ClassDescriptor d : c.getAnnotationDescriptors()) {
                        if (!d.equals(typeQualifierNickname)) {
                            resolveTypeQualifierNicknames(c.getAnnotation(d), result, onStack);
                        }
                    }
                } else if (c.getAnnotationDescriptors().contains(typeQualifier)) {
                    result.add(value);
                }
            } catch (MissingClassException e) {
                logMissingAnnotationClass(e);
                return;
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Error resolving " + annotationClass, e);
                return;
            }

        } finally {
            onStack.removeLast();
        }

    }

    public static void logMissingAnnotationClass(MissingClassException e) {
        ClassDescriptor c = e.getClassDescriptor();
        if (c.getClassName().startsWith("javax.annotation") || c.getClassName().startsWith("jakarta.annotation")) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(c);
        }
    }

    /**
     * Resolve collection of AnnotationValues (which have been used to annotate
     * an AnnotatedObject or method parameter) into collection of resolved type
     * qualifier AnnotationValues.
     *
     * @param values
     *            Collection of AnnotationValues used to annotate an
     *            AnnotatedObject or method parameter
     * @return Collection of resolved type qualifier AnnotationValues
     */
    public static Collection<AnnotationValue> resolveTypeQualifiers(Collection<AnnotationValue> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedList<AnnotationValue> result = new LinkedList<>();
        LinkedList<ClassDescriptor> onStack = new LinkedList<>();
        for (AnnotationValue value : values) {
            resolveTypeQualifierNicknames(value, result, onStack);
        }
        return result;
    }

    /**
     * Resolve an annotation into AnnotationValues representing any type
     * qualifier(s) the annotation resolves to. Detects annotations which are
     * directly marked as TypeQualifier annotations, and also resolves the use
     * of TypeQualifierNickname annotations.
     *
     * @param value
     *            AnnotationValue representing the use of an annotation
     * @param result
     *            LinkedList containing resolved type qualifier AnnotationValues
     */
    private static void resolveTypeQualifierDefaults(AnnotationValue value, ElementType defaultFor,
            LinkedList<AnnotationValue> result) {

        try {
            XClass c = Global.getAnalysisCache().getClassAnalysis(XClass.class, value.getAnnotationClass());
            AnnotationValue defaultAnnotation = c.getAnnotation(typeQualifierDefault);
            if (defaultAnnotation == null) {
                return;
            }
            for (Object o : (Object[]) defaultAnnotation.getValue("value")) {
                if (o instanceof EnumValue) {
                    EnumValue e = (EnumValue) o;
                    if (e.desc.equals(elementTypeDescriptor) && e.value.equals(defaultFor.name())) {
                        for (ClassDescriptor d : c.getAnnotationDescriptors()) {
                            if (!d.equals(typeQualifierDefault)) {
                                resolveTypeQualifierNicknames(c.getAnnotation(d), result, new LinkedList<ClassDescriptor>());
                            }
                        }
                        break;
                    }
                }
            }

        } catch (MissingClassException e) {
            logMissingAnnotationClass(e);
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error resolving " + value.getAnnotationClass(), e);

        } catch (ClassCastException e) {
            AnalysisContext.logError("ClassCastException " + value.getAnnotationClass(), e);

        }

    }

}
