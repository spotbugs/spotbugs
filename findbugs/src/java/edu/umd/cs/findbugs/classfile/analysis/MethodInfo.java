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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;

import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
public class MethodInfo extends MethodDescriptor implements XMethod {

	final int accessFlags;
	
	/**
     * @param className
     * @param methodName
     * @param methodSignature
     * @param isStatic
     */
    public MethodInfo(String className, String methodName, String methodSignature, int accessFlags) {
	    super(className, methodName, methodSignature, (accessFlags & Constants.ACC_STATIC) != 0);
	    this.accessFlags = accessFlags;
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
	    // TODO Auto-generated method stub
    	return checkFlag(Constants.ACC_SYNCHRONIZED);
    }

    public @DottedClassName String getClassName() {
	    return getClassDescriptor().toDottedClassName();
    }

    public @DottedClassName String getPackageName() {
	    return  getClassDescriptor().getPackageName();
    }

	/* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
    	if (arg0 instanceof MethodDescriptor)
    		return compareTo((MethodDescriptor)arg0);
    	else if (arg0 instanceof XMethod)
    		return compareTo((XMethod)arg0);
    	else throw new ClassCastException("Can't compare a " + this.getClass().getName() + " to a " + arg0.getClass().getName());
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getAccessFlags()
     */
    public int getAccessFlags() {
	    // TODO Auto-generated method stub
	    return accessFlags;
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isFinal()
     */
    public boolean isFinal() {
	    // TODO Auto-generated method stub
	    return checkFlag(Constants.ACC_FINAL);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPrivate()
     */
    public boolean isPrivate() {
	    // TODO Auto-generated method stub
    	return checkFlag(Constants.ACC_PRIVATE);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isProtected()
     */
    public boolean isProtected() {
	    // TODO Auto-generated method stub
    	return checkFlag(Constants.ACC_PROTECTED);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPublic()
     */
    public boolean isPublic() {
	    // TODO Auto-generated method stub
    	return checkFlag(Constants.ACC_PUBLIC);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isResolved()
     */
    public boolean isResolved() {
	    // TODO Auto-generated method stub
	    return false;
    }

}
