/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ClassWarningSuppressor;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FieldWarningSuppressor;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.MethodWarningSuppressor;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.PackageWarningSuppressor;
import edu.umd.cs.findbugs.ParameterWarningSuppressor;
import edu.umd.cs.findbugs.SuppressionMatcher;
import edu.umd.cs.findbugs.WarningSuppressor;
import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

public class NoteSuppressedWarnings extends AnnotationVisitor implements Detector, NonReportingDetector {

    private final Set<String> packages = new HashSet<>();

    /**
     * For records the header is compiled into fields, accessor methods and a canonical constructor.
     * A <code>SuppressWarnings</code> annotation in the header is applied to the corresponding field, accessor method and parameter of the canonical constructor.
     * In the end we want to report unnecessary suppressions, however when only one of the three suppressor is matched we do not want to report the two others as unnecessary.
     * We link the suppressors together so we can detect whether at least one of the was matched in {@link SuppressionMatcher#match(edu.umd.cs.findbugs.BugInstance)}
     */
    private boolean isRecord;
    private boolean visitingCanonicalRecordConstructor;
    private final List<RecordComponentSuppressors> recordComponents = new ArrayList<>();
    private final Map<String, RecordComponentSuppressors> recordComponentsByName = new HashMap<>();

    private final SuppressionMatcher suppressionMatcher;

    public NoteSuppressedWarnings(BugReporter bugReporter) {
        suppressionMatcher = AnalysisContext.currentAnalysisContext().getSuppressionMatcher();
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        isRecord = Subtypes2.isRecord(classContext.getJavaClass());

        JavaClass javaClass = classContext.getJavaClass();
        if (!BCELUtil.preTiger(javaClass)) {
            @DottedClassName
            String name = javaClass.getClassName();
            int i = name.lastIndexOf('.');
            String packageName = i < 0 ? "" : name.substring(0, i);
            if (name.endsWith(".package-info")) {
                if (!packages.add(packageName)) {
                    return;
                }
            } else if (packages.add(packageName)) {
                JavaClass packageInfoClass;
                try {
                    packageInfoClass = Repository.lookupClass(packageName + ".package-info");
                    packageInfoClass.accept(this);
                } catch (ClassNotFoundException e) {
                    assert true;
                }
            }
            javaClass.accept(this);
        }
    }

    @Override
    public void visitField(Field field) {
        if (isRecord) {
            // We first visit the records fields and track them by name and index
            RecordComponentSuppressors suppressors = new RecordComponentSuppressors(field);

            recordComponents.add(suppressors);
            recordComponentsByName.put(field.getName(), suppressors);
        }
    }

    @Override
    public void visitMethod(Method method) {
        // When we visit a record constructor with args types identical to the fields it is the canonical constructor
        visitingCanonicalRecordConstructor = isRecord
                && isConstructor(method)
                && Arrays.equals(method.getArgumentTypes(), recordComponents.stream().map(c -> c.field.getType()).toArray());
    }

    private boolean isConstructor(Method method) {
        return Const.CONSTRUCTOR_NAME.equals(method.getName());
    }

    @Override
    public void visitAfter(JavaClass obj) {
        // If we have found some suppressors on a record component link the parameter, field and method suppressors
        for (RecordComponentSuppressors suppressors : recordComponents) {
            Collection<WarningSuppressor> componentSuppressors = suppressors.getSuppressors();
            if (componentSuppressors.size() > 1) {
                for (WarningSuppressor suppressor : componentSuppressors) {
                    suppressor.addAlternateSuppressors(componentSuppressors);
                }
            }
        }

        recordComponents.clear();
    }

    @Override
    public void visitAnnotation(String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {
        if (!isSuppressWarnings(annotationClass)) {
            return;
        }
        String[] suppressed = getAnnotationParameterAsStringArray(map, "value");
        SuppressMatchType matchType = getAnnotationParameterAsEnum(map, "matchType", SuppressMatchType.class);

        if (suppressed == null || suppressed.length == 0) {
            suppressWarning(null, matchType);
        } else {
            for (String s : suppressed) {
                suppressWarning(s, matchType);
            }
        }
    }

    public static boolean isSuppressWarnings(String annotationClass) {
        return annotationClass.endsWith("SuppressWarnings")
                || annotationClass.endsWith("SuppressFBWarnings");
    }

    @Override
    public void visitParameterAnnotation(int p, String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {
        if (!isSuppressWarnings(annotationClass)) {
            return;
        }

        Method method = getMethod();
        if (!method.isStatic() && !isConstructor(method)) {
            // There's the additional 'this' parameter for non-static, non-constructor methods
            p++;
        }

        String[] suppressed = getAnnotationParameterAsStringArray(map, "value");
        SuppressMatchType matchType = getAnnotationParameterAsEnum(map, "matchType", SuppressMatchType.class);

        if (suppressed == null || suppressed.length == 0) {
            suppressParameterWarning(p, null, matchType);
        } else {
            for (String s : suppressed) {
                suppressParameterWarning(p, s, matchType);
            }
        }
    }

    private void suppressParameterWarning(int parameter, String pattern, SuppressMatchType matchType) {
        String className = getDottedClassName();
        ClassAnnotation clazz = new ClassAnnotation(className);
        ParameterWarningSuppressor suppressor = new ParameterWarningSuppressor(
                pattern,
                matchType,
                clazz,
                MethodAnnotation.fromVisitedMethod(this),
                parameter,
                MemberUtils.isUserGenerated(getXClass()));

        suppressionMatcher.addSuppressor(suppressor);

        if (visitingCanonicalRecordConstructor) {
            recordComponents.get(parameter).canonicalConstructorSuppressor = suppressor;
        }
    }

    private void suppressWarning(String pattern, SuppressMatchType matchType) {
        String className = getDottedClassName();
        ClassAnnotation clazz = new ClassAnnotation(className);
        if (className.endsWith(".package-info")) {
            PackageWarningSuppressor suppressor = new PackageWarningSuppressor(
                    pattern,
                    matchType,
                    ClassName.toDottedClassName(getPackageName()),
                    MemberUtils.isUserGenerated(getXClass()));

            suppressionMatcher.addPackageSuppressor(suppressor);
        } else if (visitingMethod()) {
            MethodWarningSuppressor suppressor = new MethodWarningSuppressor(
                    pattern,
                    matchType,
                    clazz,
                    MethodAnnotation.fromVisitedMethod(this),
                    MemberUtils.isUserGenerated(getXClass()),
                    MemberUtils.isUserGenerated(getMethod()) && MemberUtils.isUserGenerated(getXClass()));

            suppressionMatcher.addSuppressor(suppressor);

            if (isRecord) {
                RecordComponentSuppressors suppressors = recordComponentsByName.get(getMethodName());
                suppressors.accessorMethodSuppressor = suppressor;
            }
        } else if (visitingField()) {
            FieldWarningSuppressor suppressor = new FieldWarningSuppressor(
                    pattern,
                    matchType,
                    clazz,
                    FieldAnnotation.fromVisitedField(this),
                    MemberUtils.isUserGenerated(getXClass()),
                    MemberUtils.isUserGenerated(getField()) && MemberUtils.isUserGenerated(getXClass()));

            suppressionMatcher.addSuppressor(suppressor);

            if (isRecord) {
                RecordComponentSuppressors suppressors = recordComponentsByName.get(getFieldName());
                suppressors.fieldSuppressor = suppressor;
            }
        } else {
            ClassWarningSuppressor suppressor = new ClassWarningSuppressor(
                    pattern,
                    matchType,
                    clazz,
                    MemberUtils.isUserGenerated(getXClass()));

            suppressionMatcher.addSuppressor(suppressor);
        }
    }

    @Override
    public void report() {

    }

    private static class RecordComponentSuppressors {
        private final Field field;

        private WarningSuppressor canonicalConstructorSuppressor;
        private WarningSuppressor accessorMethodSuppressor;
        private WarningSuppressor fieldSuppressor;

        public RecordComponentSuppressors(Field field) {
            this.field = field;
        }

        private Collection<WarningSuppressor> getSuppressors() {
            return Stream.of(canonicalConstructorSuppressor, accessorMethodSuppressor, fieldSuppressor)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}
