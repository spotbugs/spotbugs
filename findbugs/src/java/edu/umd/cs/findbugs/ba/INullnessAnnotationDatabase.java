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

}
