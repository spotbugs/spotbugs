/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import java.util.*;

/**
 * Singleton responsible for returning localized strings for information
 * returned to the user.
 *
 * @author David Hovemeyer
 */
public class I18N {

	private final ResourceBundle messageBundle;
	private final ResourceBundle shortMessageBundle;
	private final ResourceBundle annotationDescriptionBundle;
	private final ResourceBundle bugTypeDescriptionBundle;

	private I18N() {
		messageBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsMessages");
		shortMessageBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsShortMessages");
		annotationDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsAnnotationDescriptions");
		bugTypeDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsTypeDescriptions");
	}

	private static I18N theInstance = new I18N();

	/**
	 * Get the single object instance.
	 */
	public static I18N instance() {
		return theInstance;
	}

	/**
	 * Get a message string.
	 * This is a format pattern for describing an entire bug instance in a single line.
	 * @param key which message to retrieve
	 */
	public String getMessage(String key) {
		return messageBundle.getString(key);
	}

	/**
	 * Get a short message string.
	 * This is a concrete string (not a format pattern) which briefly describes
	 * the type of bug, without mentioning particular a particular class/method/field.
	 * @param key which short message to retrieve
	 */
	public String getShortMessage(String key) {
		return shortMessageBundle.getString(key);
	}

	/**
	 * Get an annotation description string.
	 * This is a format pattern which will describe a BugAnnotation in the
	 * context of a particular bug instance.  Its single format argument
	 * is the BugAnnotation.
	 * @param key the annotation description to retrieve
	 */
	public String getAnnotationDescription(String key) {
		return annotationDescriptionBundle.getString(key);
	}

	/**
	 * Get a description for given "bug type".
	 * In this case, the bug type refers to the short prefix code prepended to
	 * the long and short bug messages.
	 * @param shortBugType the short bug type code
	 * @return the description of that short bug type code means
	 */
	public String getBugTypeDescription(String shortBugType) {
		return bugTypeDescriptionBundle.getString(shortBugType);
	}

}

// vim:ts=4
