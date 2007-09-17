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

package edu.umd.cs.findbugs.classfile;

import javax.annotation.CheckForNull;

/**
 * Exception to indicate that a resource was not found.
 * 
 * @author David Hovemeyer
 */
public class ResourceNotFoundException extends CheckedAnalysisException {
	private String resourceName;

	public static final String MESSAGE_PREFIX = "Resource not found: ";

	/**
	 * Constructor.
	 * 
	 * @param resourceName name of the missing resource
	 */
	public ResourceNotFoundException(String resourceName) {
		super(MESSAGE_PREFIX + resourceName);
		this.resourceName = resourceName;
	}

	/**
	 * Constructor.
	 * 
	 * @param resourceName name of the missing resource
	 * @param cause        underlying cause of the exception
	 */
	public ResourceNotFoundException(String resourceName, Throwable cause) {
		super(MESSAGE_PREFIX + resourceName, cause);
		this.resourceName = resourceName;
	}

	/**
	 * Get the name of the resource that was not found.
	 * 
	 * @return the name of the resource that was not found
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
     * Convert this exception to a ClassNotFoundException.
     * This method should only be called if the
     * ResourceNotFoundException occurs while looking for a class.
     * The message format is parseable by ClassNotFoundExceptionParser. 
     */
    public ClassNotFoundException toClassNotFoundException() {
    	ClassDescriptor classDescriptor = ClassDescriptor.fromResourceName(resourceName);
    	return new ClassNotFoundException(
    			"ResourceNotFoundException while looking for class " +
    			classDescriptor.toDottedClassName() +
    			": " +
    			getMessage());
    }
}
