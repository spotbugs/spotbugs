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

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * ClassInfo represents important metadata about a loaded class, such as its
 * superclass, access flags, codebase entry, etc.
 * 
 * @author David Hovemeyer
 */
public class ClassInfo extends ClassNameAndSuperclassInfo implements XClass {
	private final FieldDescriptor[] fieldDescriptorList;

	private final MethodDescriptor[] methodDescriptorList;

	private final ClassDescriptor[] referencedClassDescriptorList;

	private final ClassDescriptor immediateEnclosingClass;

	public static class Builder extends ClassNameAndSuperclassInfo.Builder {
		private FieldDescriptor[] fieldDescriptorList;

		private MethodDescriptor[] methodDescriptorList;

		private ClassDescriptor[] referencedClassDescriptorList;

		private ClassDescriptor immediateEnclosingClass;

		public ClassInfo build() {
			return new ClassInfo(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags,
			        fieldDescriptorList, methodDescriptorList, referencedClassDescriptorList, immediateEnclosingClass);
		}

		/**
		 * @return Returns the classDescriptor.
		 */
		public ClassDescriptor getClassDescriptor() {
			return classDescriptor;
		}

		/**
		 * @param fieldDescriptorList
		 *            The fieldDescriptorList to set.
		 */
		public void setFieldDescriptorList(FieldDescriptor[] fieldDescriptorList) {
			this.fieldDescriptorList = fieldDescriptorList;
		}

		/**
		 * @param methodDescriptorList
		 *            The methodDescriptorList to set.
		 */
		public void setMethodDescriptorList(MethodDescriptor[] methodDescriptorList) {
			this.methodDescriptorList = methodDescriptorList;
		}

		/**
		 * @param referencedClassDescriptorList
		 *            The referencedClassDescriptorList to set.
		 */
		public void setReferencedClassDescriptorList(ClassDescriptor[] referencedClassDescriptorList) {
			this.referencedClassDescriptorList = referencedClassDescriptorList;
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
	public ClassInfo(ClassDescriptor classDescriptor, ClassDescriptor superclassDescriptor,
	        ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags,
	        FieldDescriptor[] fieldDescriptorList, MethodDescriptor[] methodDescriptorList,
	        ClassDescriptor[] referencedClassDescriptorList, ClassDescriptor immediateEnclosingClass) {
		super(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags);
		this.fieldDescriptorList = fieldDescriptorList;
		this.methodDescriptorList = methodDescriptorList;
		this.referencedClassDescriptorList = referencedClassDescriptorList;
		this.immediateEnclosingClass = immediateEnclosingClass;
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

	/**
	 * @return Returns the referencedClassDescriptorList.
	 */
	public ClassDescriptor[] getReferencedClassDescriptorList() {
		return referencedClassDescriptorList;
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
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getClassName()
	 */
	public String getClassName() {
		return getClassDescriptor().toDottedClassName();
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

	private boolean isFlagSet(int flag) {
		return (getAccessFlags() & flag) != 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isFinal()
	 */
	public boolean isFinal() {
		return isFlagSet(IClassConstants.ACC_FINAL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPrivate()
	 */
	public boolean isPrivate() {
		return isFlagSet(IClassConstants.ACC_PRIVATE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isProtected()
	 */
	public boolean isProtected() {
		return isFlagSet(IClassConstants.ACC_PROTECTED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPublic()
	 */
	public boolean isPublic() {
		return isFlagSet(IClassConstants.ACC_PUBLIC);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isResolved()
	 */
	public boolean isResolved() {
		// A ClassInfo object only comes into existence by being loaded
		// from the classpath.
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isStatic()
	 */
	public boolean isStatic() {
		return isFlagSet(IClassConstants.ACC_STATIC);
	}
}
