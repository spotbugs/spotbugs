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
import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Bug annotation class for integer values.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class IntAnnotation implements BugAnnotation {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ROLE = "INT_DEFAULT";

    private final int value;

    private String description;

    /**
     *
     */
    public static final String INT_SYNC_PERCENT = "INT_SYNC_PERCENT";

    public static final String INT_OCCURRENCES = "INT_OCCURRENCES";

    public static final String INT_VALUE = "INT_VALUE";

    public static final String INT_MIN_VALUE = "INT_MIN_VALUE";
    public static final String INT_MAX_VALUE = "INT_MAX_VALUE";

    public static final String INT_SHIFT = "INT_SHIFT";

    public static final String INT_EXPECTED_ARGUMENTS = "INT_EXPECTED_ARGUMENTS";

    public static final String INT_ACTUAL_ARGUMENTS = "INT_ACTUAL_ARGUMENTS";

    public static final String INT_OBLIGATIONS_REMAINING = "INT_OBLIGATIONS_REMAINING";

    /**
     * Constructor.
     *
     * @param value
     *            the integer value
     */
    public IntAnnotation(int value) {
        this.value = value;
        this.description = DEFAULT_ROLE;
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
     * Get the integer value.
     *
     * @return the integer value
     */
    public int getValue() {
        return value;
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitIntAnnotation(this);
    }

    @Override
    public String format(String key, ClassAnnotation primaryClass) {
        if ("hash".equals(key)) {
            if (isSignificant()) {
                return Integer.toString(value);
            } else {
                return "";
            }
        }
        return getShortInteger(value);
    }

    public static String getShortInteger(int value) {
        String base16 = Integer.toHexString(value);
        int unique = uniqueDigits(base16);
        String base10 = Integer.toString(value);

        if (unique <= 3 && base16.length() - unique >= 3 && base10.length() > base16.length()) {
            return "0x"+base16;
        }
        return base10;
    }
    public static String getShortInteger(long value) {
        String base16 = Long.toHexString(value);
        int unique = uniqueDigits(base16);
        String base10 = Long.toString(value);

        if (unique <= 3 && base16.length() - unique >= 3 && base10.length() > base16.length()) {
            return "0x"+base16;
        }
        return base10;
    }
    private static int uniqueDigits(String value) {
        Set<Character> used = new HashSet<Character>();
        for(int i = 0; i < value.length(); i++) {
            used.add(value.charAt(i));
        }
        return used.size();
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
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntAnnotation)) {
            return false;
        }
        return value == ((IntAnnotation) o).value;
    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof IntAnnotation)) {
            // with any type of BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        return value - ((IntAnnotation) o).value;
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

    private static final String ELEMENT_NAME = "Int";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("value", String.valueOf(value));

        String role = getDescription();
        if (!DEFAULT_ROLE.equals(role)) {
            attributeList.addAttribute("role", role);
        }

        BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
    }

    @Override
    public boolean isSignificant() {
        return !INT_SYNC_PERCENT.equals(description) && !INT_OCCURRENCES.equals(description);
    }

    @Override
    public String toString(ClassAnnotation primaryClass) {
        return toString();
    }
}

