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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * A BugAnnotation object specifying a Java class involved in the bug.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 * @see BugInstance
 */
public class ClassAnnotation extends PackageMemberAnnotation {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ROLE = "CLASS_DEFAULT";

    public static final String SUBCLASS_ROLE = "CLASS_SUBCLASS";

    public static final String SUPERCLASS_ROLE = "CLASS_SUPERCLASS";

    public static final String ANONYMOUS_ROLE = "CLASS_ANONYMOUS";

    public static final String RECOMMENDED_SUPERCLASS_ROLE = "CLASS_RECOMMENDED_SUPERCLASS";

    public static final String IMPLEMENTED_INTERFACE_ROLE = "CLASS_IMPLEMENTED_INTERFACE";

    public static final String INTERFACE_ROLE = "INTERFACE_TYPE";

    public static final String ANNOTATION_ROLE = "CLASS_ANNOTATION";

    public static final String TYPE_QUALIFIER_ROLE = "CLASS_TYPE_QUALIFIER";


    /**
     * Constructor.
     *
     * @param className
     *            the name of the class
     */
    public ClassAnnotation(@DottedClassName String className) {
        super(className, DEFAULT_ROLE);
    }

    public ClassAnnotation(@DottedClassName String className, String sourceFileName) {
        super(className, DEFAULT_ROLE, sourceFileName);
    }

    @Override
    public boolean isSignificant() {
        return !SUBCLASS_ROLE.equals(description);
    }

    /**
     * Factory method to create a ClassAnnotation from a ClassDescriptor.
     *
     * @param classDescriptor
     *            the ClassDescriptor
     * @return the ClassAnnotation
     */
    public static ClassAnnotation fromClassDescriptor(ClassDescriptor classDescriptor) {
        return new ClassAnnotation(classDescriptor.toDottedClassName());
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitClassAnnotation(this);
    }

    @Override
    protected String formatPackageMember(String key, ClassAnnotation primaryClass) {
        if ("".equals(key) || "hash".equals(key)) {
            return className;
        } else if ("givenClass".equals(key)) {
            return shorten(primaryClass.getPackageName(), className);
        } else if ("excludingPackage".equals(key)) {
            return shorten(getPackageName(), className);
        } else if ("simpleClass".equals(key) || "simpleName".equals(key)) {
            return ClassName.extractSimpleName(className);
        } else {
            throw new IllegalArgumentException("unknown key " + key);
        }
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClassAnnotation)) {
            return false;
        }
        ClassAnnotation other = (ClassAnnotation) o;
        return className.equals(other.className);
    }

    public boolean contains(ClassAnnotation other) {
        return other.className.startsWith(className);
    }

    public ClassAnnotation getTopLevelClass() {
        int firstDollar = className.indexOf('$');
        if (firstDollar <= 0) {
            return this;
        }
        return new ClassAnnotation(className.substring(0, firstDollar));

    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof ClassAnnotation)) {
            // Comparable with any type of
            // BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        ClassAnnotation other = (ClassAnnotation) o;
        return className.compareTo(other.className);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.PackageMemberAnnotation#getSourceLines()
     */
    @Override
    public SourceLineAnnotation getSourceLines() {
        if (sourceLines == null) {
            this.sourceLines = getSourceLinesForClass(className, sourceFileName);
        }
        return sourceLines;
    }

    public static SourceLineAnnotation getSourceLinesForClass(@DottedClassName String className, String sourceFileName) {

        // Create source line annotation for class on demand

        AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();

        if (currentAnalysisContext == null) {
            return new SourceLineAnnotation(className, sourceFileName, -1, -1, -1, -1);
        }

        SourceInfoMap.SourceLineRange classLine = currentAnalysisContext.getSourceInfoMap().getClassLine(className);

        if (classLine == null) {
            return SourceLineAnnotation.getSourceAnnotationForClass(className, sourceFileName);
        } else {
            return new SourceLineAnnotation(className, sourceFileName, classLine.getStart(), classLine.getEnd(), -1, -1);
        }
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    private static final String ELEMENT_NAME = "Class";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("classname", getClassName());
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
