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
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * ClassInfo represents important metadata about a
 * loaded class, such as its superclass, access flags, codebase entry,
 * etc.
 * 
 * @author David Hovemeyer
 */
public class ClassInfo {
	private final ClassDescriptor classDescriptor;
	private final ClassDescriptor superclassDescriptor;
	private final ClassDescriptor[] interfaceDescriptorList;
	private final ICodeBaseEntry codeBaseEntry;
	private final int accessFlags;
	private final FieldDescriptor[] fieldDescriptorList;
	private final MethodDescriptor[] methodDescriptorList;
	private final ClassDescriptor[] referencedClassDescriptorList;
	
	public ClassInfo(
			ClassDescriptor classDescriptor,
			ClassDescriptor superclassDescriptor,
			ClassDescriptor[] interfaceDescriptorList,
			ICodeBaseEntry codeBaseEntry,
			int accessFlags,
			FieldDescriptor[] fieldDescriptorList,
			MethodDescriptor[] methodDescriptorList,
			ClassDescriptor[] referencedClassDescriptorList) {
		this.classDescriptor = classDescriptor;
		this.superclassDescriptor = superclassDescriptor;
		this.interfaceDescriptorList = interfaceDescriptorList;
		this.codeBaseEntry = codeBaseEntry;
		this.accessFlags = accessFlags;
		this.fieldDescriptorList = fieldDescriptorList;
		this.methodDescriptorList = methodDescriptorList;
		this.referencedClassDescriptorList = referencedClassDescriptorList;
	}
	
	/**
	 * @return Returns the classDescriptor.
	 */
	public ClassDescriptor getClassDescriptor() {
		return classDescriptor;
	}
	
	/**
	 * @return Returns the superclassDescriptor.
	 */
	public ClassDescriptor getSuperclassDescriptor() {
		return superclassDescriptor;
	}
	
	/**
	 * @return Returns the interfaceDescriptorList.
	 */
	public ClassDescriptor[] getInterfaceDescriptorList() {
		return interfaceDescriptorList;
	}
	
	/**
	 * @return Returns the codeBaseEntry.
	 */
	public ICodeBaseEntry getCodeBaseEntry() {
		return codeBaseEntry;
	}
	
	/**
	 * @return Returns the accessFlags.
	 */
	public int getAccessFlags() {
		return accessFlags;
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
}
