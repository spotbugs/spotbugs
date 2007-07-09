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

import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Interface for object representing information about a class.
 * 
 * @author David Hovemeyer
 */
public interface XClass extends Comparable<XClass>, AccessibleEntity {

	/**
	 * Get ClassDescriptor of this class's immediate superclass.
	 * 
     * @return ClassDescriptor of this class's immediate superclass, or
     *         null if this class has no immediate superclass
     */
    public ClassDescriptor getSuperclassDescriptor();

	/**
	 * Get ClassDescriptors of interfaces directly implemented by this class.
	 * 
     * @return ClassDescriptors of interfaces directly implemented by this class 
     */
    public ClassDescriptor[] getInterfaceDescriptorList();
    
    /**
     * Get the ClassDescriptor of the immediate enclosing class,
     * or null if this XClass is not a nested or inner class.
     * 
     * @return the ClassDescriptor of the immediate enclosing class,
     *          or null if this XClass is not a nested or inner class
     */
    public ClassDescriptor getImmediateEnclosingClass();
}
