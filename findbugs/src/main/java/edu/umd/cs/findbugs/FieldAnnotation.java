/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * A BugAnnotation specifying a particular field in particular class.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class FieldAnnotation extends PackageMemberAnnotation {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ROLE = "FIELD_DEFAULT";

    public static final String DID_YOU_MEAN_ROLE = "FIELD_DID_YOU_MEAN";

    public static final String VALUE_OF_ROLE = "FIELD_VALUE_OF";

    public static final String LOADED_FROM_ROLE = VALUE_OF_ROLE;

    public static final String STORED_ROLE = "FIELD_STORED";

    public static final String INVOKED_ON_ROLE = "FIELD_INVOKED_ON";

    public static final String ARGUMENT_ROLE = "FIELD_ARGUMENT";

    private final String fieldName;

    private final String fieldSig;

    private String fieldSourceSig;

    private final boolean isStatic;

    /**
     * Constructor.
     *
     * @param className
     *            the name of the class containing the field
     * @param fieldName
     *            the name of the field
     * @param fieldSig
     *            the type signature of the field
     */
    public FieldAnnotation(@DottedClassName String className, String fieldName, String fieldSig, boolean isStatic) {
        super(className, DEFAULT_ROLE);
        if (fieldSig.indexOf('.') >= 0) {
            assert false : "signatures should not be dotted: " + fieldSig;
        fieldSig = fieldSig.replace('.', '/');
        }
        this.fieldName = fieldName;
        this.fieldSig = fieldSig;
        this.isStatic = isStatic;
    }

    public FieldAnnotation(@DottedClassName String className, String fieldName, String fieldSig, String fieldSourceSig,
            boolean isStatic) {
        this(className, fieldName, fieldSig, isStatic);
        this.fieldSourceSig = fieldSourceSig;
    }

    /**
     * Constructor.
     *
     * @param className
     *            the name of the class containing the field
     * @param fieldName
     *            the name of the field
     * @param fieldSig
     *            the type signature of the field
     * @param accessFlags
     *            accessFlags for the field
     */
    public FieldAnnotation(@DottedClassName String className, String fieldName, String fieldSig, int accessFlags) {
        this(className, fieldName, fieldSig, (accessFlags & Constants.ACC_STATIC) != 0);
    }

    /**
     * Factory method. Class name, field name, and field signatures are taken
     * from the given visitor, which is visiting the field.
     *
     * @param visitor
     *            the visitor which is visiting the field
     * @return the FieldAnnotation object
     */
    public static FieldAnnotation fromVisitedField(PreorderVisitor visitor) {
        return new FieldAnnotation(visitor.getDottedClassName(), visitor.getFieldName(), visitor.getFieldSig(),
                visitor.getFieldIsStatic());
    }

    /**
     * Factory method. Class name, field name, and field signatures are taken
     * from the given visitor, which is visiting a reference to the field (i.e.,
     * a getfield or getstatic instruction).
     *
     * @param visitor
     *            the visitor which is visiting the field reference
     * @return the FieldAnnotation object
     */
    public static FieldAnnotation fromReferencedField(DismantleBytecode visitor) {
        String className = visitor.getDottedClassConstantOperand();
        return new FieldAnnotation(className, visitor.getNameConstantOperand(), visitor.getSigConstantOperand(),
                visitor.getRefFieldIsStatic());
    }

    /**
     * Factory method. Construct from class name and BCEL Field object.
     *
     * @param className
     *            the name of the class which defines the field
     * @param field
     *            the BCEL Field object
     * @return the FieldAnnotation
     */
    public static FieldAnnotation fromBCELField(@DottedClassName String className, Field field) {
        return new FieldAnnotation(className, field.getName(), field.getSignature(), field.isStatic());
    }

    /**
     * Factory method. Construct from class name and BCEL Field object.
     *
     * @param jClass
     *            the class which defines the field
     * @param field
     *            the BCEL Field object
     * @return the FieldAnnotation
     */
    public static FieldAnnotation fromBCELField(JavaClass jClass, Field field) {
        return new FieldAnnotation(jClass.getClassName(), field.getName(), field.getSignature(), field.isStatic());
    }

    /**
     * Factory method. Construct from a FieldDescriptor.
     *
     * @param fieldDescriptor
     *            the FieldDescriptor
     * @return the FieldAnnotation
     */
    public static FieldAnnotation fromFieldDescriptor(FieldDescriptor fieldDescriptor) {
        return new FieldAnnotation(fieldDescriptor.getClassDescriptor().getDottedClassName(), fieldDescriptor.getName(),
                fieldDescriptor.getSignature(), fieldDescriptor.isStatic());
    }

    public static FieldAnnotation fromXField(XField fieldDescriptor) {
        return new FieldAnnotation(fieldDescriptor.getClassName(), fieldDescriptor.getName(), fieldDescriptor.getSignature(),
                fieldDescriptor.getSourceSignature(), fieldDescriptor.isStatic());
    }


    public XField toXField() {
        return XFactory.createXField(className, fieldName, fieldSig, isStatic);
    }

    public FieldDescriptor toFieldDescriptor() {
        return DescriptorFactory.instance().getFieldDescriptor(this);
    }
    /**
     * Get the field name.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the type signature of the field.
     */
    public String getFieldSignature() {
        return fieldSig;
    }

    /**
     * Return whether or not the field is static.
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Is the given instruction a read of a field?
     *
     * @param ins
     *            the Instruction to check
     * @param cpg
     *            ConstantPoolGen of the method containing the instruction
     * @return the Field if the instruction is a read of a field, null otherwise
     */
    public static FieldAnnotation isRead(Instruction ins, ConstantPoolGen cpg) {
        if (ins instanceof GETFIELD || ins instanceof GETSTATIC) {
            FieldInstruction fins = (FieldInstruction) ins;
            String className = fins.getClassName(cpg);
            return new FieldAnnotation(className, fins.getName(cpg), fins.getSignature(cpg), fins instanceof GETSTATIC);
        } else {
            return null;
        }
    }

    /**
     * Is the instruction a write of a field?
     *
     * @param ins
     *            the Instruction to check
     * @param cpg
     *            ConstantPoolGen of the method containing the instruction
     * @return the Field if instruction is a write of a field, null otherwise
     */
    public static FieldAnnotation isWrite(Instruction ins, ConstantPoolGen cpg) {
        if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC) {
            FieldInstruction fins = (FieldInstruction) ins;
            String className = fins.getClassName(cpg);
            return new FieldAnnotation(className, fins.getName(cpg), fins.getSignature(cpg), fins instanceof PUTSTATIC);
        } else {
            return null;
        }
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitFieldAnnotation(this);
    }

    @Override
    protected String formatPackageMember(String key, ClassAnnotation primaryClass) {
        if ("".equals(key) || "hash".equals(key)) {
            return className + "." + fieldName;
        } else if ("givenClass".equals(key)) {
            String primaryClassName = primaryClass.getClassName();
            if (className.equals(primaryClassName)) {
                return getNameInClass(primaryClass);
            } else {
                return shorten(primaryClass.getPackageName(), className) + "." + fieldName;
            }
        } else if ("name".equals(key)) {
            return fieldName;
        } else if ("fullField".equals(key)) {
            SignatureConverter converter = new SignatureConverter(fieldSig);
            StringBuilder result = new StringBuilder();
            if (isStatic) {
                result.append("static ");
            }
            result.append(converter.parseNext());
            result.append(' ');
            result.append(className);
            result.append('.');
            result.append(fieldName);
            return result.toString();
        } else {
            throw new IllegalArgumentException("unknown key " + key);
        }
    }

    private String getNameInClass(ClassAnnotation primaryClass) {
        if (primaryClass == null) {
            return className + "." + fieldName;
        }
        String givenPackageName = primaryClass.getPackageName();
        String thisPackageName = this.getPackageName();
        if (thisPackageName.equals(givenPackageName)) {
            if (thisPackageName.length() == 0) {
                return fieldName;
            } else {
                return className.substring(thisPackageName.length() + 1) + "." + fieldName;
            }
        }
        return className + "." + fieldName;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + fieldName.hashCode() + fieldSig.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FieldAnnotation)) {
            return false;
        }
        FieldAnnotation other = (FieldAnnotation) o;
        return className.equals(other.className) && fieldName.equals(other.fieldName) && fieldSig.equals(other.fieldSig)
                && isStatic == other.isStatic;
    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof FieldAnnotation)) {
            // Comparable with any type of
            // BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        FieldAnnotation other = (FieldAnnotation) o;
        int cmp;
        cmp = className.compareTo(other.className);
        if (cmp != 0) {
            return cmp;
        }
        cmp = fieldName.compareTo(other.fieldName);
        if (cmp != 0) {
            return cmp;
        }
        return fieldSig.compareTo(other.fieldSig);
    }

    @Override
    public SourceLineAnnotation getSourceLines() {
        if (sourceLines == null) {
            // Create source line annotation for field on demand
            AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
            if (currentAnalysisContext == null) {
                sourceLines = new SourceLineAnnotation(className, sourceFileName, -1, -1, -1, -1);
            } else {
                SourceInfoMap.SourceLineRange fieldLine = currentAnalysisContext.getSourceInfoMap().getFieldLine(className,
                        fieldName);
                if (fieldLine == null) {
                    sourceLines = new SourceLineAnnotation(className, sourceFileName, -1, -1, -1, -1);
                } else {
                    sourceLines = new SourceLineAnnotation(className, sourceFileName, fieldLine.getStart(), fieldLine.getEnd(),
                            -1, -1);
                }
            }
        }
        return sourceLines;
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    private static final String ELEMENT_NAME = "Field";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("classname", getClassName())
                .addAttribute("name", getFieldName()).addAttribute("signature", getFieldSignature());
        if (fieldSourceSig != null) {
            attributeList.addAttribute("sourceSignature", fieldSourceSig);
        }
        attributeList.addAttribute("isStatic", String.valueOf(isStatic()));
        if (isPrimary) {
            attributeList.addAttribute("primary", "true");
        }

        String role = getDescription();
        if (!DEFAULT_ROLE.equals(role)) {
            attributeList.addAttribute("role", role);
        }

        xmlOutput.openTag(ELEMENT_NAME, attributeList);
        getSourceLines().writeXML(xmlOutput, addMessages, false);
        if (addMessages) {
            xmlOutput.openTag(BugAnnotation.MESSAGE_TAG);
            xmlOutput.writeText(this.toString());
            xmlOutput.closeTag(BugAnnotation.MESSAGE_TAG);
        }
        xmlOutput.closeTag(ELEMENT_NAME);
    }
}
