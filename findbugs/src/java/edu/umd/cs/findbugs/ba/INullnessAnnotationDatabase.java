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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Interface for querying nullness annotations on methods, fields,
 * and parameters.
 * 
 * @author David Hovemeyer
 */
public interface INullnessAnnotationDatabase {

	/**
	 * Determine whether given parameter must be non-null.
	 * 
	 * @param m      a method
	 * @param param  parameter (0 == first parameter)
	 * @return true if the parameter must be non-null, false otherwise
	 */
	public abstract boolean parameterMustBeNonNull(XMethod m, int param);

	/**
	 * Get a resolved NullnessAnnotation on given XMethod, XField, or XMethodParameter.
	 * 
	 * @param o          an XMethod, XField, or XMethodParameter
	 * @param getMinimal TODO: what does this mean?
	 * @return resolved NullnessAnnotation
	 */
	@CheckForNull
	public abstract NullnessAnnotation getResolvedAnnotation(final Object o, boolean getMinimal);

	/**
	 * Load "built-in" annotations that might not be evident from the
	 * analyzed/referenced code.
	 */
	public void loadAuxiliaryAnnotations();
	
	/**
	 * Add a default nullness annotation to the database.
	 * 
	 * @param cName      class name (FIXME: is this right?)
	 * @param annotation the default NullnessAnnotation
	 */
	public void addDefaultMethodAnnotation(String cName, NullnessAnnotation annotation);
	
	/**
	 * Add a field annotation to the database.
	 * 
	 * @param cName        class name
	 * @param mName        field name
	 * @param mSig         field signature
	 * @param isStatic     true if field is static, false otherwise
	 * @param annotation   NullnessAnnotation to add
	 */
	public void addFieldAnnotation(String cName, String mName, String mSig, boolean isStatic, NullnessAnnotation annotation);
	
	/**
	 * Add a field annotation to the database.
	 * 
	 * @param cName        class name
	 * @param mName        method name
	 * @param mSig         method signature
	 * @param isStatic     true if method is static, false otherwise
	 * @param annotation   NullnessAnnotation to add
	 */
	public void addMethodAnnotation(String cName, String mName, String mSig, boolean isStatic, NullnessAnnotation annotation);

	/**
	 * Add a method parameter annotation to the database.
	 * 
	 * @param cName       class name
	 * @param mName       method name
	 * @param mSig        method signature
	 * @param isStatic    true if method is static, false otherwise
	 * @param param       parameter (0 == first parameter)
	 * @param annotation  the NullnessAnnotation to add
	 */
	public void addMethodParameterAnnotation(String cName, String mName, String mSig, boolean isStatic, int param, NullnessAnnotation annotation);
	
	/**
	 * Add a default annotation to the database.
	 * 
	 * @param target one of AnnotationDatabase.METHOD, AnnotationDatabase.FIELD, AnnotationDatabase.PARAMETER, or AnnotationDatabase.ANY
	 * @param c      dotted class name of class default annotation pertains to
	 * @param n      the default NullnessAnnotation
	 */
	public void addDefaultAnnotation(String target, @DottedClassName String c, NullnessAnnotation n);
}
