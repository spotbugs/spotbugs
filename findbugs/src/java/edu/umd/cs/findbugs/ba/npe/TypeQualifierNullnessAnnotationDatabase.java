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

package edu.umd.cs.findbugs.ba.npe;

import javax.annotation.CheckForNull;
import javax.annotation.meta.When;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnnotationDatabase.Target;
import edu.umd.cs.findbugs.ba.DefaultNullnessAnnotations;
import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;
import edu.umd.cs.findbugs.ba.jsr305.FindBugsDefaultAnnotations;
import edu.umd.cs.findbugs.ba.jsr305.JSR305NullnessAnnotations;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * Implementation of INullnessAnnotationDatabase that is based on JSR-305 type
 * qualifiers.
 *
 * @author David Hovemeyer
 */
public class TypeQualifierNullnessAnnotationDatabase implements INullnessAnnotationDatabase {
    private static final boolean DEBUG = SystemProperties.getBoolean("findbugs.npe.tq.debug");

    public final TypeQualifierValue<javax.annotation.Nonnull> nonnullTypeQualifierValue;

    public TypeQualifierNullnessAnnotationDatabase() {
        this.nonnullTypeQualifierValue = TypeQualifierValue.getValue(javax.annotation.Nonnull.class, null);
    }

    @Override
    public NullnessAnnotation getResolvedAnnotation(Object o, boolean getMinimal) {
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(this.getClass());
        try {

            if (DEBUG) {
                System.out.println("getResolvedAnnotation: o=" + o + "...");
            }

            TypeQualifierAnnotation tqa = null;

            if (o instanceof XMethodParameter) {
                XMethodParameter param = (XMethodParameter) o;

                tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(param.getMethod(),
                        param.getParameterNumber(), nonnullTypeQualifierValue);
            } else if (o instanceof XMethod || o instanceof XField) {
                tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation((AnnotatedObject) o,
                        nonnullTypeQualifierValue);
            }

            NullnessAnnotation result = toNullnessAnnotation(tqa);
            if (DEBUG) {
                if (result == null) {
                    System.out.println("   ===> not found");
                } else {
                    System.out.println("   ===> " + tqa + "/" + result.toString() );
                }
            }
            return result;
        } finally {
            profiler.end(this.getClass());
        }
    }

    public @CheckForNull NullnessAnnotation getInheritedAnnotation(XMethod m, int parameter) {
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(this.getClass());
        try {
            TypeQualifierAnnotation tqa
            = TypeQualifierApplications.getInheritedTypeQualifierAnnotation(m,
                    parameter, nonnullTypeQualifierValue);
            NullnessAnnotation result = toNullnessAnnotation(tqa);
            return result;
        } finally {
            profiler.end(this.getClass());
        }
    }
    public @CheckForNull NullnessAnnotation getInheritedAnnotation(XMethod m) {
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(this.getClass());
        try {
            TypeQualifierAnnotation tqa
            = TypeQualifierApplications.getInheritedTypeQualifierAnnotation(m, nonnullTypeQualifierValue);
            NullnessAnnotation result = toNullnessAnnotation(tqa);
            return result;
        } finally {
            profiler.end(this.getClass());
        }
    }
    public @CheckForNull NullnessAnnotation getDirectAnnotation(Object o) {
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(this.getClass());
        try {

            if (DEBUG) {
                System.out.println("getDirectAnnotation: o=" + o + "...");
            }

            TypeQualifierAnnotation tqa = null;

            if (o instanceof XMethodParameter) {
                XMethodParameter param = (XMethodParameter) o;
                tqa = TypeQualifierApplications.getDirectTypeQualifierAnnotation(param.getMethod(),
                        param.getParameterNumber(), nonnullTypeQualifierValue);
            } else if (o instanceof XMethod || o instanceof XField) {
                tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation((AnnotatedObject) o,
                        nonnullTypeQualifierValue);
            }

            NullnessAnnotation result = toNullnessAnnotation(tqa);
            if (DEBUG) {
                if (result == null) {
                    System.out.println("   ===> not found");
                } else {
                    System.out.println("   ===> " + tqa + "/" + result.toString() );
                }
            }
            return result;
        } finally {
            profiler.end(this.getClass());
        }
    }

    public static boolean assertsFirstParameterIsNonnull(XMethod m) {
        return ("checkNonNull".equalsIgnoreCase(m.getName())
                || "checkNotNull".equalsIgnoreCase(m.getName())
                // JDK 7 java.util.Objects.requireNonNull(Object)
                || "requireNonNull".equals(m.getName())
                // org.eclipse.core.runtime.Assert(Object)
                || "isNotNull".equalsIgnoreCase(m.getName())
                || "assertNotNull".equalsIgnoreCase(m.getName()))
                && m.getSignature().startsWith("(Ljava/lang/Object;");
    }

    @Override
    public boolean parameterMustBeNonNull(XMethod m, int param) {
        if (DEBUG) {
            System.out.print("Checking " + m + " param " + param + " for @Nonnull...");
        }
        TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(m, param,
                nonnullTypeQualifierValue);

        if (tqa == null && param == 0) {
            String name = m.getName();
            String signature = m.getSignature();
            if ("main".equals(name) && "([Ljava/lang/String;)V".equals(signature) && m.isStatic() && m.isPublic()) {
                return true;
            } else if (assertsFirstParameterIsNonnull(m)) {
                return true;
            } else if ("compareTo".equals(name) && ")Z".equals(signature.substring(signature.indexOf(';') + 1)) && !m.isStatic()) {
                return true;
            }
        }
        boolean answer = (tqa != null) && tqa.when == When.ALWAYS;

        if (DEBUG) {
            System.out.println(answer ? "yes" : "no");
        }

        return answer;
    }

    // NOTE:
    // The way we handle adding default annotations is to actually add
    // AnnotationValues
    // to the corresponding XFoo objects, giving the illusion that the
    // annotations
    // were actually read from the underlying class files.

    /**
     * Convert a NullnessAnnotation into the ClassDescriptor of the equivalent
     * JSR-305 nullness type qualifier.
     *
     * @param n
     *            a NullnessAnnotation
     * @return ClassDescriptor of the equivalent JSR-305 nullness type qualifier
     */
    private ClassDescriptor getNullnessAnnotationClassDescriptor(NullnessAnnotation n) {
        if (n == NullnessAnnotation.CHECK_FOR_NULL) {
            return JSR305NullnessAnnotations.CHECK_FOR_NULL;
        } else if (n == NullnessAnnotation.NONNULL) {
            return JSR305NullnessAnnotations.NONNULL;
        } else if (n == NullnessAnnotation.NULLABLE) {
            return JSR305NullnessAnnotations.NULLABLE;
        } else if (n == NullnessAnnotation.UNKNOWN_NULLNESS) {
            return JSR305NullnessAnnotations.NULLABLE;
        } else {
            throw new IllegalArgumentException("Unknown NullnessAnnotation: " + n);
        }
    }

    private static final ClassDescriptor PARAMETERS_ARE_NONNULL_BY_DEFAULT = DescriptorFactory
            .createClassDescriptor(javax.annotation.ParametersAreNonnullByDefault.class);

    private static final ClassDescriptor RETURN_VALUES_ARE_NONNULL_BY_DEFAULT = DescriptorFactory
            .createClassDescriptor(edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault.class);

    @Override
    public void addDefaultAnnotation(Target target, String c, NullnessAnnotation n) {
        if (DEBUG) {
            System.out.println("addDefaultAnnotation: target=" + target + ", c=" + c + ", n=" + n);
        }

        ClassDescriptor classDesc = DescriptorFactory.instance().getClassDescriptorForDottedClassName(c);
        ClassInfo xclass;

        // Get the XClass (really a ClassInfo object)
        try {
            xclass = (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);
        } catch (MissingClassException e) {
            // AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassDescriptor());
            return;
        } catch (CheckedAnalysisException e) {
            // AnalysisContext.logError("Error adding built-in nullness annotation",
            // e);
            return;
        }
        if (n == NullnessAnnotation.NONNULL && target == AnnotationDatabase.Target.PARAMETER) {
            xclass.addAnnotation(new AnnotationValue(PARAMETERS_ARE_NONNULL_BY_DEFAULT));
            return;
        } else if (n == NullnessAnnotation.NONNULL && target == AnnotationDatabase.Target.METHOD) {
            xclass.addAnnotation(new AnnotationValue(RETURN_VALUES_ARE_NONNULL_BY_DEFAULT));
            return;
        }
        // Get the default annotation type
        ClassDescriptor defaultAnnotationType;
        if (target == AnnotationDatabase.Target.ANY) {
            defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION;
        } else if (target == AnnotationDatabase.Target.FIELD) {
            defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_FIELDS;
        } else if (target == AnnotationDatabase.Target.METHOD) {
            defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_METHODS;
        } else if (target == AnnotationDatabase.Target.PARAMETER) {
            defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_PARAMETERS;
        } else {
            throw new IllegalArgumentException("Unknown target for default annotation: " + target);
        }

        // Get the JSR-305 nullness annotation type
        ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(n);

        // Construct an AnnotationValue containing the default annotation
        AnnotationValue annotationValue = new AnnotationValue(defaultAnnotationType);
        AnnotationVisitor v = annotationValue.getAnnotationVisitor();
        v.visit("value", Type.getObjectType(nullnessAnnotationType.getClassName()));
        v.visitEnd();

        if (DEBUG) {
            System.out.println("Adding AnnotationValue " + annotationValue + " to class " + xclass);
        }

        // Destructively add the annotation to the ClassInfo object
        xclass.addAnnotation(annotationValue);
    }

    @Override
    public void addFieldAnnotation(String cName, String mName, String mSig, boolean isStatic, NullnessAnnotation annotation) {
        if (DEBUG) {
            System.out.println("addFieldAnnotation: annotate " + cName + "." + mName + " with " + annotation);
        }

        XField xfield = XFactory.createXField(cName, mName, mSig, isStatic);
        if (!(xfield instanceof FieldInfo)) {
            if (DEBUG) {
                System.out.println("  Field not found! " + cName + "." + mName + ":" + mSig + " " + isStatic + " " + annotation);
            }
            return;
        }

        // Get JSR-305 nullness annotation type
        ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);

        // Create an AnnotationValue
        AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);

        // Destructively add the annotation to the FieldInfo object
        ((FieldInfo) xfield).addAnnotation(annotationValue);
    }

    public @CheckForNull
    XMethod getXMethod(String cName, String mName, String sig, boolean isStatic) {
        ClassDescriptor classDesc = DescriptorFactory.instance().getClassDescriptorForDottedClassName(cName);
        ClassInfo xclass;

        // Get the XClass (really a ClassInfo object)
        try {
            xclass = (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);
        } catch (MissingClassException e) {
            if (DEBUG) {
                System.out.println("  Class not found!");
            }
            // AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassDescriptor());
            return null;
        } catch (CheckedAnalysisException e) {
            if (DEBUG) {
                System.out.println("  Class not found!");
            }
            // AnalysisContext.logError("Error adding built-in nullness annotation",
            // e);
            return null;
        }
        XMethod xmethod = xclass.findMethod(mName, sig, isStatic);

        if (xmethod == null) {
            xmethod = XFactory.createXMethod(cName, mName, sig, isStatic);
        }
        return xmethod;

    }

    @Override
    public void addMethodAnnotation(String cName, String mName, String sig, boolean isStatic, NullnessAnnotation annotation) {
        if (DEBUG) {
            System.out.println("addMethodAnnotation: annotate " + cName + "." + mName + " with " + annotation);
        }
        XMethod xmethod = getXMethod(cName, mName, sig, isStatic);
        if (xmethod == null) {
            return;
        }
        // Get JSR-305 nullness annotation type
        ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);

        // Create an AnnotationValue
        AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);

        // Destructively add the annotation to the MethodInfo object
        xmethod.addAnnotation(annotationValue);
    }

    @Override
    public void addMethodParameterAnnotation(@DottedClassName String cName, String mName, String sig, boolean isStatic,
            int param, NullnessAnnotation annotation) {
        if (DEBUG) {
            System.out.println("addMethodParameterAnnotation: annotate " + cName + "." + mName + " param " + param + " with "
                    + annotation);
        }
        XMethod xmethod = getXMethod(cName, mName, sig, isStatic);
        if (xmethod == null) {
            return;
        }
        // Get JSR-305 nullness annotation type
        ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);

        // Create an AnnotationValue
        AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);

        if (!xmethod.getClassName().equals(cName)) {
            if (SystemProperties.ASSERTIONS_ENABLED) {
                AnalysisContext.logError("Could not fully resolve method " + cName + "." + mName + sig + " to apply annotation "
                        + annotation);
            }
            return;
        }

        if (SystemProperties.ASSERTIONS_ENABLED) {
            SignatureParser parser = new SignatureParser(sig);
            int numParams = parser.getNumParameters();
            assert param < numParams;
        }


        // Destructively add the annotation to the MethodInfo object
        xmethod.addParameterAnnotation(param, annotationValue);
    }

    @Override
    public void loadAuxiliaryAnnotations() {
        DefaultNullnessAnnotations.addDefaultNullnessAnnotations(this);
    }

    /**
     * Convert a Nonnull-based TypeQualifierAnnotation into a
     * NullnessAnnotation.
     *
     * @param tqa
     *            Nonnull-based TypeQualifierAnnotation
     * @return corresponding NullnessAnnotation
     */
    private @CheckForNull NullnessAnnotation toNullnessAnnotation(@CheckForNull TypeQualifierAnnotation tqa) {
        if (tqa == null || tqa == TypeQualifierAnnotation.OVERRIDES_BUT_NO_ANNOTATION) {
            return null;
        }
        if (tqa.when == null) {
            new NullPointerException("TGA value with null when field").printStackTrace();
            return null;
        }



        switch (tqa.when) {
        case ALWAYS:
            return NullnessAnnotation.NONNULL;
        case MAYBE:
            return NullnessAnnotation.CHECK_FOR_NULL;
        case NEVER:
            return NullnessAnnotation.CHECK_FOR_NULL;
        case UNKNOWN:
            return NullnessAnnotation.UNKNOWN_NULLNESS;
        }

        throw new IllegalStateException();
    }
}
