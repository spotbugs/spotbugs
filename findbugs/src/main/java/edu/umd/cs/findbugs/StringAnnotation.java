/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Bug annotation class for string values.
 *
 * @author William Pugh
 * @see BugAnnotation
 */
public class StringAnnotation implements BugAnnotation {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ROLE = "STRING_DEFAULT";

    public static final String STRING_CONSTANT_ROLE = "STRING_CONSTANT";

    /** Used for non-string constants (e.g., null) */
    public static final String STRING_NONSTRING_CONSTANT_ROLE = "STRING_NONSTRING_CONSTANT";

    public static final String REGEX_ROLE = "STRING_REGEX";

    public static final String ERROR_MSG_ROLE = "STRING_ERROR_MSG";

    public static final String STRING_MESSAGE = "STRING_MESSAGE";

    public static final String PARAMETER_NAME_ROLE = "STRING_PARAMETER_NAME";

    public static final String TYPE_QUALIFIER_ROLE = "STRING_TYPE_QUALIFIER";

    public static final String REMAINING_OBLIGATIONS_ROLE = "STRING_REMAINING_OBLIGATIONS";

    public static final String FORMAT_STRING_ROLE = "STRING_FORMAT_STRING";

    public static final String FORMAT_SPECIFIER_ROLE = "STRING_FORMAT_SPECIFIER";

    final private String value;

    private String description;

    static class QuotedStringMarker {
    }

    /**
     * Constructor.
     *
     * @param value
     *            the String value
     */
    public StringAnnotation(String value) {
        this.value = value;
        this.description = DEFAULT_ROLE;
    }

    public static StringAnnotation fromRawString(String value) {
        return new StringAnnotation(Strings.escapeLFCRBackSlash(value));

    }

    public static StringAnnotation fromXMLEscapedString(String value) {
        return new StringAnnotation(Strings.unescapeXml(value));

    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Get the String value.
     *
     * @return the String value
     */
    public String getValue() {
        return value;
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitStringAnnotation(this);
    }

    @Override
    public String format(String key, ClassAnnotation primaryClass) {
        String txt = value;
        return txt;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringAnnotation)) {
            return false;
        }
        return value.equals(((StringAnnotation) o).value);
    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof StringAnnotation)) {
            // Comparable with any type of
            // BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        return value.compareTo(((StringAnnotation) o).value);
    }

    @Override
    public String toString() {
        String pattern = I18N.instance().getAnnotationDescription(description);
        FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
        return format.format(new BugAnnotation[] { this }, null);
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    private static final String ELEMENT_NAME = "String";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("value", value);

        String role = getDescription();
        if (!DEFAULT_ROLE.equals(role)) {
            attributeList.addAttribute("role", role);
        }

        BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
    }

    @Override
    public boolean isSignificant() {
        return true;
    }

    @Override
    public String toString(ClassAnnotation primaryClass) {
        return toString();
    }
}

