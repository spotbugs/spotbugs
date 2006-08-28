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

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Bug annotation class for java types.
 * This is of lighter weight than ClassAnnotation,
 * and can be used for things like array types.
 * 
 * @see ClassAnnotation
 */
public class TypeAnnotation implements BugAnnotation {
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_ROLE = "TYPE_DEFAULT";

	final private String descriptor; // jvm type descriptor, such as "[I"
	private String roleDescription;
	
	/**
	 * You probably don't want to use this constructor.
	 * Use TypeAnnotationFactory.getInstance(typeDescriptor) instead.
	 * 
	 * <p>For information on type descriptors,
	 * <br>see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#14152
	 * <br>or  http://www.murrayc.com/learning/java/java_classfileformat.shtml#TypeDescriptors
	 * 
	 * @param typeDescriptor a jvm type descriptor, such as "[I"
	 * @see TypeAnnotationFactory#getInstance(String typeDescriptor)
	 */
	public TypeAnnotation(String typeDescriptor) {
		this(typeDescriptor, DEFAULT_ROLE);
	}
	
	public TypeAnnotation(String typeDescriptor, String roleDescription) {
	    descriptor = typeDescriptor;
		this.roleDescription = roleDescription;
	}
	
	//@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("impossible", e);
		}
	}

	/**
	 * Get the type descriptor.
	 * @return the jvm type descriptor, such as "[I"
	 */
	public String getTypeDescriptor() {
		return descriptor;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitTypeAnnotation(this);
	}

	/** change a type descriptor such as "[I"
	 *  to a source-code-style type such as"int[]".
	 *  If the descriptor can't be parsed, it will be returned verbatim. */
	public static String typeDescriptorToSourceType(String descriptor) {
		/* note 1: I figured a method that does this must already
		           exist in the findbugs code base but, if so, I
		           was unable to find it.
		   note 2: I shoud probably do better error handling than
		           just returning the descriptor verbatim.     */
		if (descriptor == null) return descriptor;
		int adim = 0, len = descriptor.length(), j = 0;
		while (j < len && descriptor.charAt(j)=='[') {
			adim += 1;
			j += 1;
		}
		if (j >= len) return descriptor; // descriptor is something like "[[["
		StringBuffer sb = new StringBuffer(len+adim);
		switch (descriptor.charAt(j)) {
			case 'B': sb.append("byte");
                      break;
			case 'C': sb.append("char");
                      break;
			case 'D': sb.append("double");
                      break;
			case 'F': sb.append("float");
			          break;
			case 'I': sb.append("int");
			          break;
			case 'J': sb.append("long");
			          break;
			case 'S': sb.append("short");
			          break;
			case 'Z': sb.append("boolean");
	                  break;
			case 'L': int semi = descriptor.indexOf(';', j);
			          if (semi < 0) semi = len; // hmm, someone forgot the final semicolon?
			          if (semi < j+2) return descriptor;
			          String slashed = descriptor.substring(j+1, semi);
			          String dotted = slashed.replace('/', '.');
			          sb.append(dotted);
			          j = semi;
	                  break;
			 default: return descriptor; // unknown char
		}
		if (j+1 < len) {
			// shoud complain here, but for now we'll just ignore the extra chars
		}
		while (adim-- > 0) sb.append("[]");
		return sb.toString();
	}

	public String format(String key) {
		return typeDescriptorToSourceType(descriptor);
	}

	public void setDescription(String roleDescription) {
		this.roleDescription = roleDescription;
	}

	public String getDescription() {
		return roleDescription;
	}

	public int hashCode() {
		return descriptor.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TypeAnnotation))
			return false;
		return descriptor.equals(((TypeAnnotation) o).descriptor);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof TypeAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return descriptor.compareTo(((TypeAnnotation) o).descriptor);
		// could try to determine equivalence with ClassAnnotation, but don't see how this would be useful
	}

	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(roleDescription);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this});
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Type";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("descriptor", descriptor);
		
		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);
		
		BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
	}
}
