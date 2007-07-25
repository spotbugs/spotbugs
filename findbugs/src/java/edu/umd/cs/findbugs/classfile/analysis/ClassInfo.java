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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.classfile.analysis;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.Util;

/**
 * ClassInfo represents important metadata about a loaded class, such as its
 * superclass, access flags, codebase entry, etc.
 * 
 * @author David Hovemeyer
 */
public class ClassInfo extends ClassNameAndSuperclassInfo implements XClass, AnnotatedObject {
	private final FieldDescriptor[] fieldDescriptorList;

	private final MethodDescriptor[] methodDescriptorList;

	private final ClassDescriptor immediateEnclosingClass;

	final Map<ClassDescriptor, AnnotationValue> classAnnotations;
	final private String classSourceSignature;


	public static class Builder extends ClassNameAndSuperclassInfo.Builder {
		private List<FieldDescriptor>fieldDescriptorList = new LinkedList<FieldDescriptor>();

		private List<MethodDescriptor> methodDescriptorList  = new LinkedList<MethodDescriptor>();


		private ClassDescriptor immediateEnclosingClass;
		final Map<ClassDescriptor, AnnotationValue> classAnnotations = new HashMap<ClassDescriptor, AnnotationValue>();
		private String classSourceSignature;


		public ClassInfo build() {
			return new ClassInfo(classDescriptor,classSourceSignature, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags, 
					referencedClassDescriptorList,classAnnotations,
					fieldDescriptorList.toArray(new FieldDescriptor[0]), methodDescriptorList.toArray(new MethodDescriptor[0]), 
					immediateEnclosingClass );
		}

		/**
		 * @return Returns the classDescriptor.
		 */
		public ClassDescriptor getClassDescriptor() {
			return classDescriptor;
		}

		public void setSourceSignature(String classSourceSignature) {
			this.classSourceSignature = classSourceSignature;
		}
		public void addAnnotation(String name, AnnotationValue value) {
			ClassDescriptor annotationClass = ClassDescriptor.createClassDescriptorFromSignature(name);
			classAnnotations.put(annotationClass, value);
		}
		/**
		 * @param fieldDescriptorList
		 *            The fieldDescriptorList to set.
		 */
		public void setFieldDescriptorList(FieldDescriptor[] fieldDescriptorList) {
			this.fieldDescriptorList = Arrays.asList(fieldDescriptorList);
		}
		public void addFieldDescriptor(FieldDescriptor field) {
			fieldDescriptorList.add(field);
		}

		/**
		 * @param methodDescriptorList
		 *            The methodDescriptorList to set.
		 */
		public void setMethodDescriptorList(MethodDescriptor[] methodDescriptorList) {
			this.methodDescriptorList = Arrays.asList(methodDescriptorList);
		}
		public void addMethodDescriptor(MethodDescriptor method) {
			methodDescriptorList.add(method);
		}

		/**
		 * @param immediateEnclosingClass
		 *            The immediateEnclosingClass to set.
		 */
		public void setImmediateEnclosingClass(ClassDescriptor immediateEnclosingClass) {
			this.immediateEnclosingClass = immediateEnclosingClass;
		}

	}

	/**
	 * 
	 * @param classDescriptor
	 *            ClassDescriptor representing the class name
	 * @param superclassDescriptor
	 *            ClassDescriptor representing the superclass name
	 * @param interfaceDescriptorList
	 *            ClassDescriptors representing implemented interface names
	 * @param codeBaseEntry
	 *            codebase entry class was loaded from
	 * @param accessFlags
	 *            class's access flags
	 * @param fieldDescriptorList
	 *            FieldDescriptors of fields defined in the class
	 * @param methodDescriptorList
	 *            MethodDescriptors of methods defined in the class
	 * @param referencedClassDescriptorList
	 *            ClassDescriptors of all classes/interfaces referenced by the
	 *            class
	 */
	private ClassInfo(ClassDescriptor classDescriptor, String classSourceSignature, ClassDescriptor superclassDescriptor,
			ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags,
			Collection<ClassDescriptor> referencedClassDescriptorList,
			Map<ClassDescriptor, AnnotationValue> classAnnotations,
			FieldDescriptor[] fieldDescriptorList, MethodDescriptor[] methodDescriptorList,
			ClassDescriptor immediateEnclosingClass) {
		super(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags, referencedClassDescriptorList);
		this.classSourceSignature = classSourceSignature;
		this.fieldDescriptorList = fieldDescriptorList;
		this.methodDescriptorList = methodDescriptorList;
		this.immediateEnclosingClass = immediateEnclosingClass;
		this.classAnnotations = Util.immutableMap(classAnnotations);
	}

	/**
	 * @return Returns the fieldDescriptorList.
	 */
	public FieldDescriptor[] getFieldDescriptorList() {
		return fieldDescriptorList;
	}

	/**
	 * @return Returns the methodDescriptorList.
	 */
	public MethodDescriptor[] getMethodDescriptorList() {
		return methodDescriptorList;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findMethod(java.lang.String, java.lang.String, boolean)
	 */
	public XMethod findMethod(String methodName, String methodSig, boolean isStatic) {
		for (MethodDescriptor mDesc : methodDescriptorList) {
			if (mDesc instanceof MethodInfo) {
				MethodInfo mInfo = (MethodInfo) mDesc;
				if (mInfo.getName().equals(methodName)
						&& mInfo.getSignature().equals(methodSig)
						&& mInfo.isStatic() == isStatic) {
					return mInfo;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findMethod(edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	public XMethod findMethod(MethodDescriptor descriptor) {
		if (!descriptor.getClassDescriptor().equals(this)) {
			throw new IllegalArgumentException();
		}
		return findMethod(descriptor.getName(), descriptor.getSignature(), descriptor.isStatic());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findField(java.lang.String, java.lang.String, boolean)
	 */
	public XField findField(String name, String signature, boolean isStatic) {
		for (FieldDescriptor fDesc : fieldDescriptorList) {
			if (fDesc instanceof FieldInfo) {
				FieldInfo fInfo = (FieldInfo) fDesc;
				if (fInfo.getName().equals(name)
						&& fInfo.getSignature().equals(signature)
						&& fInfo.isStatic() == isStatic) {
					return fInfo;
				}
			}
		}
		return null;
	}

	/**
	 * @return Returns the immediateEnclosingClass.
	 */
	public ClassDescriptor getImmediateEnclosingClass() {
		return immediateEnclosingClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getPackageName()
	 */
	public String getPackageName() {
		String dottedClassName = getClassDescriptor().toDottedClassName();
		int lastDot = dottedClassName.lastIndexOf('.');
		if (lastDot < 0) {
			return "";
		} else {
			return dottedClassName.substring(0, lastDot);
		}
	}
	public Collection<ClassDescriptor> getAnnotationDescriptors() {
		return classAnnotations.keySet();
	}
	public Collection<AnnotationValue> getAnnotations() {
		return classAnnotations.values();
	}
	public AnnotationValue getAnnotation(ClassDescriptor desc) {
		return classAnnotations.get(desc);
	}

	public ElementType getElementType() {
		if (getClassName().endsWith("package-info")) return ElementType.PACKAGE;
		else if (isAnnotation()) return ElementType.ANNOTATION_TYPE;
		return ElementType.TYPE;
		
	}
	
	public @CheckForNull AnnotatedObject getContainingScope() {
		try {
			if (immediateEnclosingClass != null) {
				return (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, getImmediateEnclosingClass());
			}
			if (getClassName().endsWith("/package-info") || getClassName().equals("package-info")) {
				return null;
			}
			ClassDescriptor p = ClassDescriptor.createClassDescriptorFromDottedClassName(getPackageName() +"."+"package-info");
			return (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, p);
		} catch (CheckedAnalysisException e) {
			return null;
		}
	}

}
