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

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * FindBugs-specific default-annotation annotations.
 * I.e.:
 * <pre>
 *   {@literal @DefaultAnnotationForParameters({Nonnull.class})}
 *   public class MyClass {
 *      ...
 *   }
 * </pre>
 * @author David Hovemeyer
 */
public abstract class FindBugsDefaultAnnotations {

	/** Default annotation for all element types. */
	public static final ClassDescriptor DEFAULT_ANNOTATION =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/DefaultAnnotation");

	/** Default annotation for fields. */
	public static final ClassDescriptor DEFAULT_ANNOTATION_FOR_FIELDS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/DefaultAnnotationForFields");

	/** Default annotation for methods. */
	public static final ClassDescriptor DEFAULT_ANNOTATION_FOR_METHODS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/DefaultAnnotationForMethods");

	/** Default annotation for parameters. */
	public static final ClassDescriptor DEFAULT_ANNOTATION_FOR_PARAMETERS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/DefaultAnnotationForParameters");

}
