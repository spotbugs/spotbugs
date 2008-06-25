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

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
public class UnresolvedXField extends AbstractField implements XField {

	protected UnresolvedXField(@DottedClassName String className, String methodName, String methodSig, int accessFlags) {
		super(className, methodName, methodSig, accessFlags);
		if (XFactory.DEBUG_UNRESOLVED) {
			System.out.println("Unresolved xmethod: " + this);
		}
	}
	protected UnresolvedXField(FieldDescriptor m) {
		super(m.getClassDescriptor().getDottedClassName(), m.getName(), m.getSignature(), 0);
		if (XFactory.DEBUG_UNRESOLVED) {
			System.out.println("Unresolved xmethod: " + this);
		}
	}
	/* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
    	if (o instanceof XField) {
			return XFactory.compare((XField)this, (XField)o);
		}
		throw new ClassCastException("Don't know how to compare " + this.getClass().getName() + " to " + o.getClass().getName());

    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject#getAnnotation(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    public AnnotationValue getAnnotation(ClassDescriptor desc) {
	    return null;
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject#getAnnotationDescriptors()
     */
    public Collection<ClassDescriptor> getAnnotationDescriptors() {
	    return  Collections.<ClassDescriptor>emptyList();
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject#getAnnotations()
     */
    public Collection<AnnotationValue> getAnnotations() {
	    return Collections.<AnnotationValue>emptyList();
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject#getContainingScope()
     */
    public AnnotatedObject getContainingScope() {
	    // TODO Auto-generated method stub
	    return AnalysisContext.currentXFactory().getXClass(DescriptorFactory.createClassDescriptorFromDottedClassName(getClassName()));
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject#getElementType()
     */
    public ElementType getElementType() {
	    return ElementType.FIELD;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isDeprecated()
     */
    public boolean isDeprecated() {
	    // TODO Auto-generated method stub
	    return false;
    }

}
