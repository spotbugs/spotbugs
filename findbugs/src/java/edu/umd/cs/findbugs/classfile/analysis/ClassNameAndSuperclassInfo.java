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

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

/**
 * Represents the class name, superclass name, and interface list
 * of a class. 
 * 
 * @author David Hovemeyer
 */
public class ClassNameAndSuperclassInfo {
	private ClassDescriptor classDescriptor;
	private ClassDescriptor superclassDescriptor;
	private ClassDescriptor[] interfaceDescriptorList;
	private ICodeBaseEntry codeBaseEntry;
	private int accessFlags;

	/**
	 * Constructor.
	 * Does not initialize any fields; setters should be called
	 * before the object is used.
	 */
	public ClassNameAndSuperclassInfo() {
	}

	/**
	 * Constructor.
	 * 
	 * @param classDescriptor         ClassDescriptor representing the class name
	 * @param superclassDescriptor    ClassDescriptor representing the superclass name
	 * @param interfaceDescriptorList ClassDescriptors representing implemented interface names
	 * @param codeBaseEntry           codebase entry class was loaded from
	 * @param accessFlags             class's access flags
	 */
	public ClassNameAndSuperclassInfo(ClassDescriptor classDescriptor, ClassDescriptor superclassDescriptor, ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags) {
		this.classDescriptor = classDescriptor;
		this.superclassDescriptor = superclassDescriptor;
		this.interfaceDescriptorList = interfaceDescriptorList;
		this.codeBaseEntry = codeBaseEntry;
		this.accessFlags = accessFlags;
	}

	/**
	 * @return Returns the accessFlags.
	 */
	public int getAccessFlags() {
		return accessFlags;
	}

	/**
	 * @param accessFlags The accessFlags to set.
	 */
	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	/**
	 * @return Returns the classDescriptor.
	 */
	public ClassDescriptor getClassDescriptor() {
		return classDescriptor;
	}

	/**
	 * @param classDescriptor The classDescriptor to set.
	 */
	public void setClassDescriptor(ClassDescriptor classDescriptor) {
		this.classDescriptor = classDescriptor;
	}

	/**
	 * @return Returns the codeBaseEntry.
	 */
	public ICodeBaseEntry getCodeBaseEntry() {
		return codeBaseEntry;
	}

	/**
	 * @param codeBaseEntry The codeBaseEntry to set.
	 */
	public void setCodeBaseEntry(ICodeBaseEntry codeBaseEntry) {
		this.codeBaseEntry = codeBaseEntry;
	}

	/**
	 * @return Returns the interfaceDescriptorList.
	 */
	public ClassDescriptor[] getInterfaceDescriptorList() {
		return interfaceDescriptorList;
	}

	/**
	 * @param interfaceDescriptorList The interfaceDescriptorList to set.
	 */
	public void setInterfaceDescriptorList(ClassDescriptor[] interfaceDescriptorList) {
		this.interfaceDescriptorList = interfaceDescriptorList;
	}

	/**
	 * @return Returns the superclassDescriptor.
	 */
	public ClassDescriptor getSuperclassDescriptor() {
		return superclassDescriptor;
	}

	/**
	 * @param superclassDescriptor The superclassDescriptor to set.
	 */
	public void setSuperclassDescriptor(ClassDescriptor superclassDescriptor) {
		this.superclassDescriptor = superclassDescriptor;
	}

}
