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
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class FieldInfo extends FieldDescriptor implements XField, AnnotatedObject {

	static public class Builder {
		final int accessFlags;

		final String className, fieldName, fieldSignature;

		String fieldSourceSignature;

		final Map<ClassDescriptor, AnnotationValue> fieldAnnotations = new HashMap<ClassDescriptor, AnnotationValue>();

		public Builder(@SlashedClassName String className, String fieldName, String fieldSignature, int accessFlags) {
			this.className = className;
			this.fieldName = fieldName;
			this.fieldSignature = fieldSignature;
			this.accessFlags = accessFlags;
		}

		public void setSourceSignature(String fieldSourceSignature) {
			this.fieldSourceSignature = fieldSourceSignature;
		}

		public void addAnnotation(String name, AnnotationValue value) {
			ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
			fieldAnnotations.put(annotationClass, value);
		}

		public FieldInfo build() {
			return new FieldInfo(className, fieldName, fieldSignature, fieldSourceSignature, accessFlags, fieldAnnotations, true);
		}
	}

	final int accessFlags;

	final @CheckForNull String fieldSourceSignature;
	Map<ClassDescriptor, AnnotationValue> fieldAnnotations;

	final boolean isResolved;
	
	/**
     * @param className
     * @param fieldName
     * @param fieldSignature
     * @param isStatic
     * @param accessFlags
     * @param fieldAnnotations
     * @param isResolved
     */
    private FieldInfo(
    		@SlashedClassName String className,
    		String fieldName,
    		String fieldSignature,
    		@CheckForNull String fieldSourceSignature,
    		int accessFlags,
    		Map<ClassDescriptor, AnnotationValue> fieldAnnotations,
    		boolean isResolved) {
	    super(className, fieldName, fieldSignature, (accessFlags & Constants.ACC_STATIC) != 0);
	    this.accessFlags = accessFlags | (fieldName.startsWith("this$") ? Constants.ACC_FINAL : 0);
		this.fieldSourceSignature = fieldSourceSignature;
		this.fieldAnnotations = Util.immutableMap(fieldAnnotations);
		this.isResolved = isResolved;
    }


    public int getNumParams() {
    	return new SignatureParser(getSignature()).getNumParameters();
    }

    private boolean checkFlag(int flag) {
    	return (accessFlags & flag) != 0;
    }

    public boolean isNative() {
	    return checkFlag(Constants.ACC_NATIVE);
    }


    public boolean isSynchronized() {
    	return checkFlag(Constants.ACC_SYNCHRONIZED);
    }

    public @DottedClassName String getClassName() {
	    return getClassDescriptor().toDottedClassName();
    }

    public @DottedClassName String getPackageName() {
	    return  getClassDescriptor().getPackageName();
    }
	public String getSourceSignature() {
		return fieldSourceSignature;
	}

	/* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object rhs) {
    	if (rhs instanceof FieldDescriptor) {
    		return super.compareTo((FieldDescriptor) rhs);
    	}
    	
    	if (rhs instanceof XField) {
    		return XFactory.compare((XField) this, (XField) rhs); 
    	}
    	
    	throw new ClassCastException("Can't compare a " + this.getClass().getName() + " to a " + rhs.getClass().getName());
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getAccessFlags()
     */
    public int getAccessFlags() {
	    return accessFlags;
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isFinal()
     */
    public boolean isFinal() {
	    return checkFlag(Constants.ACC_FINAL);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPrivate()
     */
    public boolean isPrivate() {
    	return checkFlag(Constants.ACC_PRIVATE);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isProtected()
     */
    public boolean isProtected() {
    	return checkFlag(Constants.ACC_PROTECTED);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPublic()
     */
    public boolean isPublic() {
    	return checkFlag(Constants.ACC_PUBLIC);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isResolved()
     */
    public boolean isResolved() {
	    return this.isResolved;
    }


	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XField#isReferenceType()
     */
    public boolean isReferenceType() {
		return getSignature().startsWith("L") || getSignature().startsWith("[");
	}


	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XField#isVolatile()
     */
    public boolean isVolatile() {
	    return checkFlag(Constants.ACC_VOLATILE);
    }

    public Collection<ClassDescriptor> getAnnotationDescriptors() {
		return fieldAnnotations.keySet();
	}
	public AnnotationValue getAnnotation(ClassDescriptor desc) {
		return fieldAnnotations.get(desc);
	}
	public Collection<AnnotationValue> getAnnotations() {
		return fieldAnnotations.values();
	}
	
	/**
	 * Destructively add an annotation.
	 * We do this for "built-in" annotations that might not
	 * be directly evident in the code.
	 * It's not a great idea in general, but we can
	 * get away with it as long as it's done early
	 * enough (i.e., before anyone asks what annotations
	 * this field has.) 
	 * 
	 * @param annotationValue an AnnotationValue representing a field annotation
	 */
	public void addAnnotation(AnnotationValue annotationValue) {
		HashMap<ClassDescriptor, AnnotationValue> updatedAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(fieldAnnotations);
		updatedAnnotations.put(annotationValue.getAnnotationClass(), annotationValue);
		fieldAnnotations = updatedAnnotations;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XField#getFieldDescriptor()
	 */
	public FieldDescriptor getFieldDescriptor() {
		return this;
	}
	
	/**
	 * Create a FieldInfo object to represent an unresolved field.
	 * <em>Don't call this directly - use XFactory instead.</em>
	 * 
	 * @param className  name of class containing the field
	 * @param name       name of field
	 * @param signature  field signature
	 * @param isStatic   true if field is static, false otherwise
	 * @return FieldInfo object representing the unresolved field
	 */
	public static FieldInfo createUnresolvedFieldInfo(String className, String name, String signature, boolean isStatic) {
		className = ClassName.toSlashedClassName(className);
		return new FieldInfo(
				className,
				name,
				signature,
				null, // without seeing the definition we don't know if it has a generic type
				isStatic ? Constants.ACC_STATIC : 0,
				new HashMap<ClassDescriptor, AnnotationValue>(),
				false);
	}
	
	public ElementType getElementType() {
		return ElementType.FIELD;
	}
	public @CheckForNull AnnotatedObject getContainingScope() {
		try {
	        return (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
        } catch (CheckedAnalysisException e) {
	         return null;
        }
	}
}
