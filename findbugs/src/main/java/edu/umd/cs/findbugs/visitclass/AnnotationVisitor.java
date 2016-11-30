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
package edu.umd.cs.findbugs.visitclass;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.ParameterAnnotations;
import org.apache.bcel.classfile.SimpleElementValue;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Subclass of PreorderVisitor that visits annotations on classes, fields,
 * methods, and method parameters.
 *
 * @author William Pugh
 */
public class AnnotationVisitor extends PreorderVisitor {

    static final boolean DEBUG = SystemProperties.getBoolean("annotation.visitor");

    /**
     * Visit annotation on a class, field or method
     *
     * @param annotationClass
     *            class of annotation
     * @param map
     *            map from names to values
     * @param runtimeVisible
     *            true if annotation is runtime visible
     */
    public void visitAnnotation(@DottedClassName String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {
        if (DEBUG) {
            System.out.println("Annotation: " + annotationClass);
            for (Map.Entry<String, ElementValue> e : map.entrySet()) {
                System.out.println("    " + e.getKey());
                System.out.println(" -> " + e.getValue());
            }
        }
    }

    protected static String getAnnotationParameterAsString(Map<String, ElementValue> map, String parameter) {
        try {
            ElementValue ev = map.get(parameter);

            if (ev instanceof SimpleElementValue) {
                return ((SimpleElementValue) ev).getValueString();
            }
            return null;
        } catch (Exception e) {
            return null;

        }
    }

    @CheckForNull
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    protected static String[] getAnnotationParameterAsStringArray(Map<String, ElementValue> map, String parameter) {
        try {
            ElementValue e = map.get(parameter);
            ArrayElementValue a = (ArrayElementValue) e;
            int size = a.getElementValuesArraySize();
            String[] result = new String[size];
            ElementValue[] elementValuesArray = a.getElementValuesArray();
            for (int i = 0; i < size; i++) {
                result[i] = ((SimpleElementValue) elementValuesArray[i]).getValueString();
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Visit annotation on a method parameter
     *
     * @param p
     *            parameter number, starting at zero ("this" parameter is not
     *            counted)
     * @param annotationClass
     *            class of annotation
     * @param map
     *            map from names to values
     * @param runtimeVisible
     *            true if annotation is runtime visible
     */
    public void visitParameterAnnotation(int p, @DottedClassName String annotationClass, Map<String, ElementValue> map,
            boolean runtimeVisible) {

    }

    public void visitSyntheticParameterAnnotation(int p, boolean runtimeVisible) {
    }

    /*

    private static final String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations";
    private static final String RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";
    private static final String RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    private static final String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations";

    private Map<String, Object> readAnnotationValues(DataInputStream bytes, int numPairs) throws IOException {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int j = 0; j < numPairs; j++) {
            int memberNameIndex = bytes.readUnsignedShort();
            String memberName = ((ConstantUtf8) getConstantPool().getConstant(memberNameIndex)).getBytes();
            if (DEBUG) {
                System.out.println("memberName: " + memberName);
            }
            Object value = readAnnotationValue(bytes);
            if (DEBUG) {
                System.out.println(memberName + ":" + value);
            }
            values.put(memberName, value);
        }
        return values;
    }


    private @DottedClassName
    String getAnnotationName(DataInputStream bytes) throws IOException {
        int annotationNameIndex = bytes.readUnsignedShort();
        String annotationName = ((ConstantUtf8) getConstantPool().getConstant(annotationNameIndex)).getBytes().replace('/', '.');
        annotationName = annotationName.substring(1, annotationName.length() - 1);
        if (DEBUG) {
            System.out.println("Annotation name: " + annotationName);
        }
        return annotationName;
    }


    private Object readAnnotationValue(DataInputStream bytes) throws IOException {
        try {
            char tag = (char) bytes.readUnsignedByte();
            if (DEBUG) {
                System.out.println("tag: " + tag);
            }
            switch (tag) {
            case '[': {
                int sz = bytes.readUnsignedShort();
                if (DEBUG) {
                    System.out.println("Array of " + sz + " entries");
                }
                Object[] result = new Object[sz];
                for (int i = 0; i < sz; i++) {
                    result[i] = readAnnotationValue(bytes);
                }
                return result;
            }
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 's':
            case 'c':
                int cp_index = bytes.readUnsignedShort();
                Constant c = getConstantPool().getConstant(cp_index);
                switch (tag) {
                case 'B':
                    return (byte) ((ConstantInteger) c).getBytes();
                case 'C':
                    return (char) ((ConstantInteger) c).getBytes();
                case 'D':
                    return new Double(((ConstantDouble) c).getBytes());
                case 'F':
                    return new Float(((ConstantFloat) c).getBytes());
                case 'I':
                    return ((ConstantInteger) c).getBytes();
                case 'J':
                    return ((ConstantLong) c).getBytes();
                case 'S':
                    return (char) ((ConstantInteger) c).getBytes();
                case 'Z':
                    return Boolean.valueOf(((ConstantInteger) c).getBytes() != 0);
                case 's':
                    return ((ConstantUtf8) c).getBytes();
                case 'c':
                    String cName = ((ConstantUtf8) c).getBytes().replace('/', '.');
                    if (cName.startsWith("L") && cName.endsWith(";")) {
                        cName = cName.substring(1, cName.length() - 1);
                    }
                    if (DEBUG) {
                        System.out.println("cName: " + cName);
                    }
                    return cName;
                default:
                    if (DEBUG) {
                        System.out.println("Impossible");
                    }
                    throw new IllegalStateException("Impossible");
                }
            case '@':
                throw new IllegalArgumentException("Not ready to handle annotations as elements of annotations");
            case 'e': {
                int cp1 = bytes.readUnsignedShort();
                ConstantUtf8 c1 = (ConstantUtf8) getConstantPool().getConstant(cp1);
                String cName = c1.getBytes().replace('/', '.');
                if (cName.startsWith("L") && cName.endsWith(";")) {
                    cName = cName.substring(1, cName.length() - 1);
                }
                int cp2 = bytes.readUnsignedShort();
                ConstantUtf8 c2 = (ConstantUtf8) getConstantPool().getConstant(cp2);
                String result = cName + "." + c2.getBytes();
                // System.out.println(result);
                return result;
            }
            default:
                if (DEBUG) {
                    System.out.println("Unexpected tag of " + tag);
                }
                throw new IllegalArgumentException("Unexpected tag of " + tag);
            }
        } catch (RuntimeException e) {
            if (DEBUG) {
                System.out.println("Problem processing annotation " + e.getMessage());
                e.printStackTrace();
            }
            throw e;
        }
    }
     */

    @Override
    public void visitParameterAnnotation(ParameterAnnotations arg0) {
        ParameterAnnotationEntry[] parameterAnnotationEntries = arg0.getParameterAnnotationEntries();
        int numParametersToMethod = getNumberMethodArguments();
        int offset = 0;
        if (numParametersToMethod > parameterAnnotationEntries.length) {
            offset = 1;
        }
        for (int i = 0; i < parameterAnnotationEntries.length; i++) {
            ParameterAnnotationEntry e = parameterAnnotationEntries[i];
            for (AnnotationEntry ae : e.getAnnotationEntries()) {
                boolean runtimeVisible = ae.isRuntimeVisible();

                String name = ClassName.fromFieldSignature(ae.getAnnotationType());
                if (name == null) {
                    continue;
                }
                name = ClassName.toDottedClassName(name);
                Map<String, ElementValue> map = new HashMap<String, ElementValue>();
                for (ElementValuePair ev : ae.getElementValuePairs()) {
                    map.put(ev.getNameString(), ev.getValue());
                }
                visitParameterAnnotation(offset + i, name, map, runtimeVisible);

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitAnnotation(org.apache.bcel.classfile
     * .Annotations)
     */
    @Override
    public void visitAnnotation(Annotations arg0) {
        for (AnnotationEntry ae : arg0.getAnnotationEntries()) {
            boolean runtimeVisible = ae.isRuntimeVisible();
            String name = ClassName.fromFieldSignature(ae.getAnnotationType());
            if (name == null) {
                continue;
            }
            name = ClassName.toDottedClassName(name);
            Map<String, ElementValue> map = new HashMap<String, ElementValue>();
            for (ElementValuePair ev : ae.getElementValuePairs()) {
                map.put(ev.getNameString(), ev.getValue());
            }
            visitAnnotation(name, map, runtimeVisible);

        }

    }
}
