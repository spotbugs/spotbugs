/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs;

import java.io.IOException;

import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Bug annotation class for java types. This is of lighter weight than
 * ClassAnnotation, and can be used for things like array types.
 *
 * @see ClassAnnotation
 */
public class TypeAnnotation extends BugAnnotationWithSourceLines {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ROLE = "TYPE_DEFAULT";

    public static final String EXPECTED_ROLE = "TYPE_EXPECTED";

    public static final String FOUND_ROLE = "TYPE_FOUND";

    public static final String CLOSEIT_ROLE = "TYPE_CLOSEIT";

    public static final String UNHASHABLE_ROLE = "TYPE_UNHASHABLE";

    final private String descriptor; // jvm type descriptor, such as "[I"

    private String roleDescription;

    private String typeParameters;

    /**
     * constructor.
     *
     * <p>
     * For information on type descriptors, <br>
     * see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.
     * html#14152 <br>
     * or http://www.murrayc.com/learning/java/java_classfileformat.shtml#
     * TypeDescriptors
     *
     * @param typeDescriptor
     *            a jvm type descriptor, such as "[I"
     */
    public TypeAnnotation(String typeDescriptor) {
        this(typeDescriptor, DEFAULT_ROLE);
    }

    public TypeAnnotation(Type objectType) {
        this(objectType, DEFAULT_ROLE);
    }

    public TypeAnnotation(Type objectType, String roleDescription) {
        this(objectType.getSignature(), roleDescription);
        if (objectType instanceof GenericObjectType) {
            GenericObjectType genericObjectType = (GenericObjectType) objectType;
            if (genericObjectType.getTypeCategory() == GenericUtilities.TypeCategory.PARAMETERIZED) {
                typeParameters = genericObjectType.getGenericParametersAsString();
            }
        }
    }

    public TypeAnnotation(String typeDescriptor, String roleDescription) {
        descriptor = typeDescriptor;
        this.roleDescription = roleDescription;
        if (descriptor.startsWith("L")) {
            String className = typeDescriptor.substring(1, typeDescriptor.length() - 1).replace('/', '.');
            AnalysisContext context = AnalysisContext.currentAnalysisContext();
            if (context != null) {
                this.sourceFileName = context.lookupSourceFile(className);
                this.sourceLines = ClassAnnotation.getSourceLinesForClass(className, sourceFileName);
            } else {
                this.sourceFileName = SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
            }
        }
    }

    /**
     * Get the type descriptor.
     *
     * @return the jvm type descriptor, such as "[I"
     */
    public String getTypeDescriptor() {
        return descriptor;
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitTypeAnnotation(this);
    }

    @Override
    public String format(String key, ClassAnnotation primaryClass) {
        String name = new SignatureConverter(descriptor).parseNext().replace("java.lang.", "");
        if ("givenClass".equals(key)) {
            name = PackageMemberAnnotation.shorten(primaryClass.getPackageName(), name);
        } else if ("excludingPackage".equals(key)) {
            name = PackageMemberAnnotation.removePackage(name);
        }

        if (typeParameters != null && !"hash".equals(key)) {
            name = name + typeParameters;
        }
        return name;
    }

    @Override
    public void setDescription(String roleDescription) {
        this.roleDescription = roleDescription.intern();
    }

    @Override
    public String getDescription() {
        return roleDescription;
    }

    public void setTypeParameters(String typeParameters) {
        this.typeParameters = typeParameters;
    }

    public String getTypeParameters() {
        return typeParameters;
    }

    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeAnnotation)) {
            return false;
        }
        return descriptor.equals(((TypeAnnotation) o).descriptor);
    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof TypeAnnotation)) {
            // with any type of BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        return descriptor.compareTo(((TypeAnnotation) o).descriptor);
        // could try to determine equivalence with ClassAnnotation, but don't
        // see how this would be useful
    }

    @Override
    public String toString() {
        String pattern = I18N.instance().getAnnotationDescription(roleDescription);
        FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
        return format.format(new BugAnnotation[] { this }, null);
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    private static final String ELEMENT_NAME = "Type";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("descriptor", descriptor);

        String role = getDescription();
        if (!DEFAULT_ROLE.equals(role)) {
            attributeList.addAttribute("role", role);
        }
        if (typeParameters != null) {
            attributeList.addAttribute("typeParameters", typeParameters);
        }

        BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
    }

    @Override
    public boolean isSignificant() {
        return true;
    }
}
