/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.Serializable;

/**
 * An object providing context information about a particular BugInstance.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public interface BugAnnotation
		extends Comparable<BugAnnotation>, XMLWriteableWithMessages, Serializable, Cloneable {
	/**
	 * XML tag for a formatted text message describing the annotation. 
	 */
	public static final String MESSAGE_TAG = "Message";

	public Object clone();

	/**
	 * Accept a BugAnnotationVisitor.
	 *
	 * @param visitor the visitor to accept
	 */
	public void accept(BugAnnotationVisitor visitor);

	/**
	 * Format the annotation as a String.
	 * The given key specifies additional information about how the annotation should
	 * be formatted.  If the key is empty, then the "default" format will be used.
	 *
	 * @param key how the annotation should be formatted
	 * @param primaryClass TODO
	 */
	public String format(String key, ClassAnnotation primaryClass);

	/**
	 * Get a description of this bug annotation.
	 * The description is a key for the FindBugsAnnotationDescriptions resource bundle.
	 */
	public String getDescription();

	/**
	 * Set a description of this bug annotation.
	 * The description is a key for the FindBugsAnnotationDescriptions resource bundle.
	 */
	public void setDescription(String description);

	/**
	 * Is this annotation used to compute instance hashes or match bug instances across versions
	 * @return true if significant
	 */
	public boolean isSignificant();
}

// vim:ts=4
