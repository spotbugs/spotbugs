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

package edu.umd.cs.findbugs.ba.jsr305;

import edu.umd.cs.findbugs.ba.AnnotationEnumeration;

/**
 * Representation of a type qualifier annotation
 * (an annotation that is annotated with the javax.annotation.meta.Qualifier annotation).
 * 
 * @author David Hovemeyer
 */
public class TypeQualifier extends AnnotationEnumeration<TypeQualifier> {
	/**
	 * When value of this instance of the qualifier.
	 */
	private final When when;
	// Do we need to represent applicableTo?

	TypeQualifier(String className, When when) {
		super(className, when.getIndex());
		this.when = when;
	}

	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return super.toString();
	}

	/**
	 * @return Returns the when.
	 */
	public When getWhen() {
		return when;
	}
}
