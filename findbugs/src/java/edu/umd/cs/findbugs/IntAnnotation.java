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

	private int value;
	private String description;

	/**
	 * 
	 */
	public static final String INT_SYNC_PERCENT = "INT_SYNC_PERCENT";
	public static final String INT_OCCURRENCES = "INT_OCCURRENCES";
    public static final String INT_VALUE = "INT_VALUE";
    public static final String INT_SHIFT= "INT_SHIFT";

	/**
	 * Constructor.
	 *
	 * @param value the integer value
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

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitIntAnnotation(this);
	}

	public String format(String key, ClassAnnotation primaryClass) {
		if (!isSignificant()) return "";
		return String.valueOf(value);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IntAnnotation))
			return false;
		return value == ((IntAnnotation) o).value;
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof IntAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return value - ((IntAnnotation) o).value;
	}

	@Override
	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this}, null);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Int";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("value", String.valueOf(value));
		
		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);
		
		BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
	}
 
	public boolean isSignificant() {
		return !description.equals(INT_SYNC_PERCENT) && !description.equals(INT_OCCURRENCES);
	}
}

// vim:ts=4
