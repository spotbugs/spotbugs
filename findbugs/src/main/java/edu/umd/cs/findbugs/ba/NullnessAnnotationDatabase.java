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

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.npe.TypeQualifierNullnessAnnotationDatabase;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * @author pugh
 */
@Deprecated
public class NullnessAnnotationDatabase extends AnnotationDatabase<NullnessAnnotation> implements INullnessAnnotationDatabase {

    public NullnessAnnotationDatabase() {
        setAddClassOnly(true);
        loadAuxiliaryAnnotations();
        setAddClassOnly(false);
    }

    @Override
    public void loadAuxiliaryAnnotations() {
        DefaultNullnessAnnotations.addDefaultNullnessAnnotations(this);
    }

    @Override
    public boolean parameterMustBeNonNull(XMethod m, int param) {
        if (param == 0) {
            if ("equals".equals(m.getName()) && "(Ljava/lang/Object;)Z".equals(m.getSignature()) && !m.isStatic()) {
                return false;
            } else if ("main".equals(m.getName()) && "([Ljava/lang/String;)V".equals(m.getSignature()) && m.isStatic()
                    && m.isPublic()) {
                return true;
            } else if (TypeQualifierNullnessAnnotationDatabase.assertsFirstParameterIsNonnull(m)) {
                return true;
            } else if ("compareTo".equals(m.getName()) && m.getSignature().endsWith(";)Z") && !m.isStatic()) {
                return true;
            }
        }
        if (!anyAnnotations(NullnessAnnotation.NONNULL)) {
            return false;
        }
        XMethodParameter xmp = new XMethodParameter(m, param);
        NullnessAnnotation resolvedAnnotation = getResolvedAnnotation(xmp, true);


        return resolvedAnnotation == NullnessAnnotation.NONNULL;
    }

    @CheckForNull
    @Override
    public NullnessAnnotation getResolvedAnnotation(final Object o, boolean getMinimal) {

        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(this.getClass());
        try {
            if (o instanceof XMethodParameter) {
                XMethodParameter mp = (XMethodParameter) o;
                XMethod m = mp.getMethod();
                // TODO: Handle argument to equals specially: generate special
                // bug code for it
                int parameterNumber = mp.getParameterNumber();
                if (parameterNumber == 0) {
                    if ("equals".equals(m.getName()) && "(Ljava/lang/Object;)Z".equals(m.getSignature()) && !m.isStatic()) {
                        return NullnessAnnotation.CHECK_FOR_NULL;
                    } else if ("main".equals(m.getName()) && "([Ljava/lang/String;)V".equals(m.getSignature()) && m.isStatic()
                            && m.isPublic()) {
                        return NullnessAnnotation.NONNULL;
                    } else if (TypeQualifierNullnessAnnotationDatabase.assertsFirstParameterIsNonnull(m)) {
                        return NullnessAnnotation.NONNULL;
                    } else if ("compareTo".equals(m.getName()) && m.getSignature().endsWith(";)Z") && !m.isStatic()) {
                        return NullnessAnnotation.NONNULL;
                    }
                }
            } else if (o instanceof XMethod) {
                XMethod m = (XMethod) o;
                String name = m.getName();
                String signature = m.getSignature();
                if (!m.isStatic()
                        && ("clone".equals(name) && "()Ljava/lang/Object;".equals(signature) || "toString".equals(name)
                                && "()Ljava/lang/String;".equals(signature) || m.isPrivate() && "readResolve".equals(name)
                                && "()Ljava/lang/Object;".equals(signature))) {
                    NullnessAnnotation result = super.getDirectAnnotation(m);
                    if (result != null) {
                        return result;
                    }
                    return NullnessAnnotation.NONNULL;
                }

            } else if (o instanceof XField) {
                XField f = (XField) o;
                if (f.getName().startsWith("this$")) {
                    return NullnessAnnotation.NONNULL;
                }
            }
            NullnessAnnotation result = super.getResolvedAnnotation(o, getMinimal);
            return result;
        } finally {
            profiler.end(this.getClass());
        }
    }



    @Override
    public void addDefaultMethodAnnotation(String name, NullnessAnnotation annotation) {
        super.addDefaultMethodAnnotation(name, annotation);
    }

    @Override
    public void addDefaultAnnotation(AnnotationDatabase.Target target, String c, NullnessAnnotation n) {
        super.addDefaultAnnotation(target, c, n);
    }

    @Override
    public void addFieldAnnotation(String name, String name2, String sig, boolean isStatic, NullnessAnnotation annotation) {
        super.addFieldAnnotation(name, name2, sig, isStatic, annotation);
    }

    @Override
    public void addMethodAnnotation(String name, String name2, String sig, boolean isStatic, NullnessAnnotation annotation) {
        super.addMethodAnnotation(name, name2, sig, isStatic, annotation);
    }

    @Override
    public void addMethodParameterAnnotation(String name, String name2, String sig, boolean isStatic, int param,
            NullnessAnnotation annotation) {
        super.addMethodParameterAnnotation(name, name2, sig, isStatic, param, annotation);
    }
}
