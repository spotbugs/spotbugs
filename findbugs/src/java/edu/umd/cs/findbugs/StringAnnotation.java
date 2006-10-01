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
 * @author William Pugh
 * @see BugAnnotation
 */
public class StringAnnotation implements BugAnnotation {
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_ROLE = "STRING_DEFAULT";

	final private String value;
	 private String description;

	/**
	 * Constructor.
	 *
	 * @param value the String value
	 */
	public StringAnnotation(String value) {
		this.value = quoteCharacters(value);
		this.description = DEFAULT_ROLE;
	}
	
	//@Override
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("impossible", e);
		}
	}

	
	 private static String quoteCharacters(String s) {
	        StringBuffer result = null;
	        for(int i = 0, max = s.length(), delta = 0; i < max; i++) {
	            char c = s.charAt(i);
	            String replacement = null;

	            if (c == '&') {
	                replacement = "&amp;";
	            } else if (c == '<') {
	                replacement = "&lt;";
	            } else if (c == '\r') {
	                replacement = "&#13;";
	            } else if (c == '>') {
	                replacement = "&gt;";
	            } else if (c == '"') {
	                replacement = "&quot;";
	            } else if (c == '\'') {
	                replacement = "&apos;";
	            }

	            if (replacement != null) {
	                if (result == null) {
	                    result = new StringBuffer(s);
	                }
	                result.replace(i + delta, i + delta + 1, replacement);
	                delta += (replacement.length() - 1);
	            }
	        }
	        if (result == null) {
	            return s;
	        }
	        return result.toString();
	    }

	 
	/**
	 * Get the String value.
	 *
	 * @return the String value
	 */
	public String getValue() {
		return value;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitStringAnnotation(this);
	}

	public String format(String key, ClassAnnotation primaryClass) {
		return value;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof StringAnnotation))
			return false;
		return value.equals(((StringAnnotation) o).value);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof StringAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return value.compareTo(((StringAnnotation) o).value);
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

	private static final String ELEMENT_NAME = "String";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("value", value);
		
		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);
		
		BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
	}


	public boolean isSignificant() {
		return true;
	}
}

// vim:ts=4
