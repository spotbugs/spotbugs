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

package edu.umd.cs.findbugs.classfile.analysis;

import java.lang.annotation.ElementType;
import java.util.Collection;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * @author pwilliam
 */
public interface AnnotatedObject {
	public Collection<ClassDescriptor> getAnnotationDescriptors();
	
	/** return the ClassDescriptor for the class associated with this object */
	public ClassDescriptor getClassDescriptor();
	public  @Nullable AnnotationValue getAnnotation(ClassDescriptor desc);
	public Collection<AnnotationValue> getAnnotations();
	
	public ElementType getElementType();
	public @CheckForNull AnnotatedObject getContainingScope();
}
