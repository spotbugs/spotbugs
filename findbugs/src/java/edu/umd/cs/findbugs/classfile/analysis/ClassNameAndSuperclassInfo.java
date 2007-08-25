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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

/**
 * Represents the class name, superclass name, and interface list of a class.
 * 
 * @author David Hovemeyer
 */
public class ClassNameAndSuperclassInfo extends ClassDescriptor  {
	private final ClassDescriptor superclassDescriptor;

	private final ClassDescriptor[] interfaceDescriptorList;

	private final ICodeBaseEntry codeBaseEntry;

	private final int accessFlags;
	private final Collection<ClassDescriptor> referencedClassDescriptorList,calledClassDescriptorList;

	public static class Builder {
		ClassDescriptor classDescriptor;

		ClassDescriptor superclassDescriptor;

		ClassDescriptor[] interfaceDescriptorList;

		ICodeBaseEntry codeBaseEntry;

		int accessFlags;
		
		Collection<ClassDescriptor> referencedClassDescriptorList;
		Collection<ClassDescriptor>  calledClassDescriptorList = Collections.emptyList();

		public ClassNameAndSuperclassInfo build() {
			return new ClassNameAndSuperclassInfo(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry,
			        accessFlags,referencedClassDescriptorList, calledClassDescriptorList);
		}

		/**
		 * @param accessFlags
		 *            The accessFlags to set.
		 */
		public void setAccessFlags(int accessFlags) {
			this.accessFlags = accessFlags;
		}

		/**
		 * @param classDescriptor
		 *            The classDescriptor to set.
		 */
		public void setClassDescriptor(ClassDescriptor classDescriptor) {
			this.classDescriptor = classDescriptor;
		}

		/**
		 * @param codeBaseEntry
		 *            The codeBaseEntry to set.
		 */
		public void setCodeBaseEntry(ICodeBaseEntry codeBaseEntry) {
			this.codeBaseEntry = codeBaseEntry;
		}

		/**
		 * @param interfaceDescriptorList
		 *            The interfaceDescriptorList to set.
		 */
		public void setInterfaceDescriptorList(ClassDescriptor[] interfaceDescriptorList) {
			this.interfaceDescriptorList = interfaceDescriptorList;
		}

		/**
		 * @param superclassDescriptor
		 *            The superclassDescriptor to set.
		 */
		public void setSuperclassDescriptor(ClassDescriptor superclassDescriptor) {
			this.superclassDescriptor = superclassDescriptor;
		}

		/**
		 * @param referencedClassDescriptorList
		 *            The referencedClassDescriptorList to set.
		 */
		public void setReferencedClassDescriptorList(Collection<ClassDescriptor> referencedClassDescriptorList) {
			this.referencedClassDescriptorList = referencedClassDescriptorList;
		}
		public void setCalledClassDescriptorList(Collection<ClassDescriptor> calledClassDescriptorList) {
			this.calledClassDescriptorList = calledClassDescriptorList;
		}
	}

	/**
	 * Constructor.
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
	 */
	 ClassNameAndSuperclassInfo(ClassDescriptor classDescriptor, ClassDescriptor superclassDescriptor,
	        ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags, Collection<ClassDescriptor> referencedClassDescriptorList, 
	        Collection<ClassDescriptor> calledClassDescriptorList) {
		super(classDescriptor.getClassName());
		this.superclassDescriptor = superclassDescriptor;
		this.interfaceDescriptorList = interfaceDescriptorList;
		this.codeBaseEntry = codeBaseEntry;
		this.accessFlags = accessFlags;
		this.referencedClassDescriptorList = referencedClassDescriptorList;
		if (calledClassDescriptorList == null)
			throw new NullPointerException("calledCBelassDescriptorList must not be null");
		this.calledClassDescriptorList = calledClassDescriptorList;
	}

	/**
	 * @return Returns the accessFlags.
	 */
	public int getAccessFlags() {
		return accessFlags;
	}

	/**
	 * @return Returns the classDescriptor.
	 */
	public ClassDescriptor getClassDescriptor() {
		return this;
	}

	/**
	 * @return Returns the codeBaseEntry.
	 */
	public ICodeBaseEntry getCodeBaseEntry() {
		return codeBaseEntry;
	}

	/**
	 * @return Returns the interfaceDescriptorList.
	 */
	public ClassDescriptor[] getInterfaceDescriptorList() {
		return interfaceDescriptorList;
	}
	/**
	 * @return Returns the referenced class descriptor list.
	 */
	public Collection<ClassDescriptor> getReferencedClassDescriptorList() {
		return referencedClassDescriptorList;
	}
	/**
	 * @return Returns the called class descriptor list.
	 */
	public Collection<ClassDescriptor> getCalledClassDescriptorList() {
		return calledClassDescriptorList;
	}
	/**
	 * @return Returns the superclassDescriptor.
	 */
	public ClassDescriptor getSuperclassDescriptor() {
		return superclassDescriptor;
	}

	private boolean isFlagSet(int flag) {
    	return (getAccessFlags() & flag) != 0;
    }

	public boolean isFinal() {
    	return isFlagSet(IClassConstants.ACC_FINAL);
    }

	public boolean isPrivate() {
    	return isFlagSet(IClassConstants.ACC_PRIVATE);
    }

	public boolean isProtected() {
    	return isFlagSet(IClassConstants.ACC_PROTECTED);
    }

	public boolean isPublic() {
    	return isFlagSet(IClassConstants.ACC_PUBLIC);
    }

	public boolean isStatic() {
    	return isFlagSet(IClassConstants.ACC_STATIC);
    }

	public boolean isInterface() {
    	return isFlagSet(IClassConstants.ACC_INTERFACE);
    }
	public boolean isAnnotation() {
		return isFlagSet(IClassConstants.ACC_ANNOTATION);
	}

}
