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

package edu.umd.cs.findbugs.classfile.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.engine.asm.FindBugsASM;

/**
 * The "raw" version of an annotation appearing in a class file.
 *
 * @author William Pugh
 */
public class AnnotationValue {
    private final ClassDescriptor annotationClass;

    private final Map<String, Object> valueMap = new HashMap<String, Object>(4);

    private final Map<String, Object> typeMap = new HashMap<String, Object>(4);

    /**
     * Constructor.
     *
     * @param annotationClass
     *            the annotation class
     */
    public AnnotationValue(ClassDescriptor annotationClass) {
        this.annotationClass = annotationClass;
    }

    /**
     * Constructor.
     *
     * @param annotationClass
     *            JVM signature of the annotation class
     */
    public AnnotationValue(String annotationClass) {
        this.annotationClass = DescriptorFactory.createClassDescriptorFromSignature(annotationClass);
    }

    /**
     * @return ClassDescriptor referring to the annotation class
     */
    public ClassDescriptor getAnnotationClass() {
        return annotationClass;
    }

    /**
     * Get the value of given annotation element. See <a href=
     * "http://asm.objectweb.org/current/doc/javadoc/user/org/objectweb/asm/AnnotationVisitor.html"
     * >AnnotationVisitor Javadoc</a> for information on what the object
     * returned could be.
     *
     * @param name
     *            name of annotation element
     * @return the element value (primitive value, String value, enum value,
     *         Type, or array of one of the previous)
     */
    public Object getValue(String name) {
        return valueMap.get(name);
    }

    /**
     * Get a descriptor specifying the type of an annotation element.
     *
     * @param name
     *            name of annotation element
     * @return descriptor specifying the type of the annotation element
     */
    public Object getDesc(String name) {
        return typeMap.get(name);
    }

    @Override
    public String toString() {
        return annotationClass + ":" + valueMap.toString();
    }

    private static String canonicalString(String s) {
        if ("value".equals(s)) {
            return "value";
        }
        return s;
    }

    /**
     * Get an AnnotationVisitor which can populate this AnnotationValue object.
     */
    public AnnotationVisitor getAnnotationVisitor() {
        return new AnnotationVisitor(FindBugsASM.ASM_VERSION) {
            @Override
            public void visit(String name, Object value) {
                name = canonicalString(name);
                valueMap.put(name, value);
            }

            /*
             * (non-Javadoc)
             *
             * @see
             * org.objectweb.asm.AnnotationVisitor#visitAnnotation(java.lang
             * .String, java.lang.String)
             */
            @Override
            public AnnotationVisitor visitAnnotation(String name, String desc) {
                name = canonicalString(name);
                AnnotationValue newValue = new AnnotationValue(desc);
                valueMap.put(name, newValue);
                typeMap.put(name, desc);
                return newValue.getAnnotationVisitor();
            }

            /*
             * (non-Javadoc)
             *
             * @see
             * org.objectweb.asm.AnnotationVisitor#visitArray(java.lang.String)
             */
            @Override
            public AnnotationVisitor visitArray(String name) {
                name = canonicalString(name);
                return new AnnotationArrayVisitor(name);
            }

            /*
             * (non-Javadoc)
             *
             * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
             */
            @Override
            public void visitEnd() {

            }

            /*
             * (non-Javadoc)
             *
             * @see
             * org.objectweb.asm.AnnotationVisitor#visitEnum(java.lang.String,
             * java.lang.String, java.lang.String)
             */
            @Override
            public void visitEnum(String name, String desc, String value) {
                name = canonicalString(name);
                valueMap.put(name, new EnumValue(desc, value));
                typeMap.put(name, desc);

            }
        };
    }

    private final class AnnotationArrayVisitor extends AnnotationVisitor {
        /**
         *
         */
        private final String name;

        private final List<Object> outerList;

        /**
         *
         */
        private final List<Object> result = new LinkedList<Object>();

        /**
         * @param name
         * @param result
         */
        private AnnotationArrayVisitor(String name) {
            super(FindBugsASM.ASM_VERSION);
            name = canonicalString(name);
            this.name = name;
            this.outerList = null;
        }

        private AnnotationArrayVisitor(List<Object> outerList) {
            super(FindBugsASM.ASM_VERSION);
            this.name = null;
            this.outerList = outerList;
        }

        @Override
        public void visit(String name, Object value) {
            result.add(value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            AnnotationValue newValue = new AnnotationValue(desc);
            result.add(newValue);
            return newValue.getAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AnnotationArrayVisitor(result);
        }

        @Override
        public void visitEnd() {
            if (name != null) {
                valueMap.put(name, result.toArray());
            } else {
                outerList.add(result.toArray());
            }
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            result.add(new EnumValue(desc, value));

        }
    }

}
