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

import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;

/**
 * The result of looking up a TypeQualifierAnnotation.
 * Because type qualifiers are inherited, a full result of
 * looking resolving a TypeQualifierAnnotation
 * may include annotations on one or more supertypes.
 * Potentially, the supertype annotations may conflict with
 * each other, and/or conflict with the annotation on the annotated
 * entity.  This object makes it possible to report
 * such conflicts, while still providing a convenient
 * interface for getting the "effective" TypeQualifierAnnotation.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierAnnotationLookupResult {
	/**
	 * Partial result of looking up a TypeQualifierAnnotation.
	 */
	public static class PartialResult {
		private AnnotatedObject annotatedObject;
		private TypeQualifierAnnotation typeQualifierAnnotation;

		PartialResult(AnnotatedObject annotatedObject, TypeQualifierAnnotation typeQualifierAnnotation) {
			this.annotatedObject = annotatedObject;
			this.typeQualifierAnnotation = typeQualifierAnnotation;
		}

		/**
		 * @return Returns the annotatedObject.
		 */
		public AnnotatedObject getAnnotatedObject() {
			return annotatedObject;
		}

		/**
		 * @return Returns the typeQualifierAnnotation.
		 */
		public TypeQualifierAnnotation getTypeQualifierAnnotation() {
			return typeQualifierAnnotation;
		}
	}

	private List<PartialResult> partialResultList;

	TypeQualifierAnnotationLookupResult() {
		this.partialResultList = new LinkedList<PartialResult>();
	}
	
	void addPartialResult(PartialResult partialResult) {
		partialResultList.add(partialResult);
	}

	/**
	 * Get the effective TypeQualifierAnnotation.
	 * 
	 * @return the effective TypeQualifierAnnotation,
	 *         or null if no effective TypeQualifierAnnotation
	 *         can be found
	 */
	public @CheckForNull TypeQualifierAnnotation getEffectiveTypeQualifierAnnotation() {
		boolean firstPartialResult = true;
		TypeQualifierAnnotation effective = null;

		for (PartialResult partialResult : partialResultList) {
			if (firstPartialResult) {
				effective = partialResult.getTypeQualifierAnnotation();
				firstPartialResult = false;
			} else {
				effective = combine(effective, partialResult.getTypeQualifierAnnotation());
			}
		}
		
		return effective;
	}

	/**
	 * Subclasses must override this method to combine TypeQualifierAnnotations
	 * found in multiple superclasses.
	 * 
	 * @param a a TypeQualifierAnnotation
	 * @param b another TypeQualifierAnnotation
	 * @return combined TypeQualifierAnnotation compatible with both input TypeQualifierAnnotations,
	 *         or null if no such TypeQualifierAnnotation exists
	 */
	protected TypeQualifierAnnotation combine(TypeQualifierAnnotation a, TypeQualifierAnnotation b) {
		return null;
	}
}
