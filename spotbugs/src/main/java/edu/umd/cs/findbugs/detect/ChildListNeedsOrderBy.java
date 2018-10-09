/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Administrator
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

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * @since ?
 *
 */
public class ChildListNeedsOrderBy implements Detector {

    BugReporter bugReporter;

    public ChildListNeedsOrderBy(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();

        if(!isEntityClass(javaClass)) {
            return;
        }

        Field[] fields = javaClass.getFields();

        if (null == fields || 0 == fields.length) {
            return;
        }

        for (Field field : fields) {
            String fieldType = field.getType().toString();
            boolean isElementCollection = false;
            boolean isOrderBy = false;

            if (!fieldType.endsWith("List")) {
                continue;
            }

            AnnotationEntry[] annotationEntries = field.getAnnotationEntries();
            if (null == annotationEntries) {
                continue;
            }
            for (AnnotationEntry entry : annotationEntries) {
                String entryInfo = entry.toShortString();
                if (entryInfo.contains("ElementCollection")) {
                    isElementCollection = true;
                }
                if (entryInfo.contains("OrderBy")) {
                    isOrderBy = true;
                }
            }

            String className = javaClass.getClassName();
            if (isElementCollection && !isOrderBy) {
                bugReporter.reportBug(new BugInstance(this, "SPEC_CHILDLIST_MAY_NEEDS_ORDERBY_ANNOTATION", LOW_PRIORITY)
                        .addClass(className).addField(new FieldAnnotation(className, field.getName(),
                                field.getSignature(), field.isStatic())));
            }
        }
    }

    /**
     * @param javaClass
     * @return
     */
    private boolean isEntityClass(JavaClass javaClass) {
        AnnotationEntry[] classAnnotationEntries = javaClass.getAnnotationEntries();
        if (null == classAnnotationEntries || 0 == classAnnotationEntries.length) {
            return false;
        }

        for (AnnotationEntry entry : classAnnotationEntries) {
            String annotationType = entry.getAnnotationType();
            if (annotationType.contains("Entity")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void report() {

    }

}
