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

package edu.umd.cs.findbugs.ba.ch;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.graph.AbstractVertex;

/**
 * Vertex class - represents a class or interface in the InheritanceGraph.
 * Edges connect subtypes to supertypes.
 * 
 * @author David Hovemeyer
 */
class ClassVertex extends AbstractVertex<InheritanceEdge, ClassVertex> {
	private ClassDescriptor classDescriptor;
	private XClass xclass;
	private boolean finished;
	private boolean applicationClass;

	/**
	 * Constructor.
	 * 
	 * @param classDescriptor ClassDescriptor naming the class or interface
	 * @param xclass          object containing information about a class or interface,
	 *                        null if this ClassVertex represents a missing class
	 */
	public ClassVertex(ClassDescriptor classDescriptor, @Nullable XClass xclass) {
		this.classDescriptor = classDescriptor;
		this.xclass = xclass;
		this.finished = false;
		this.applicationClass = false;
	}
	
	/**
     * @return Returns the classDescriptor.
     */
    public ClassDescriptor getClassDescriptor() {
	    return classDescriptor;
    }

	/**
	 * @return Returns the xClass.
	 */
	public XClass getXClass() {
		return xclass;
	}

	/**
	 * Return true if this ClassVertex corresponds to a resolved
	 * class, or false if the class could not be found.
	 */
	public boolean isResolved() {
		return xclass != null;
	}

	/**
	 * @param finished The finished to set.
	 */
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * @return Returns the finished.
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
     * Mark this ClassVertex as representing an application class.
     */
    public void markAsApplicationClass() {
    	this.applicationClass = true;
    }
    
    /**
     * @return true if this ClassVertex represents an application class,
     *         false otherwise
     */
    public boolean isApplicationClass() {
	    return applicationClass;
    }
}
