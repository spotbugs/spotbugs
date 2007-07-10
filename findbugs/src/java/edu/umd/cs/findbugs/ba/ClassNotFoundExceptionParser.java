/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Parse the detail message in a ClassNotFoundException
 * to extract the name of the missing class.
 * Unfortunately, this information is not directly available
 * from the exception object.  So, this class parses the
 * detail message in several common formats (such as the
 * format used by BCEL).
 *
 * @author David Hovemeyer
 */
public class ClassNotFoundExceptionParser {
	// BCEL reports missing classes in this format
	private static final Pattern BCEL_MISSING_CLASS_PATTERN =
			Pattern.compile("^.*while looking for class ([^:]*):.*$");

	// edu.umd.cs.findbugs.ba.type.TypeRepository
	// and edu.umd.cs.findbugs.ba.ch.Subtypes2 uses this format
	private static final Pattern TYPE_REPOSITORY_MISSING_CLASS_PATTERN =
			Pattern.compile("^Class ([^ ]*) cannot be resolved.*$");

	private static final Pattern[] patternList;

	static {
		ArrayList<Pattern> list = new ArrayList<Pattern>();
		list.add(BCEL_MISSING_CLASS_PATTERN);
		list.add(TYPE_REPOSITORY_MISSING_CLASS_PATTERN);

		patternList = list.toArray(new Pattern[list.size()]);
	}

	/**
	 * Get the name of the missing class from a ClassNotFoundException.
	 *
	 * @param ex the ClassNotFoundException
	 * @return the name of the missing class, or null if we
	 *         couldn't figure out the class name
	 */
	public static String getMissingClassName(ClassNotFoundException ex) {
		// If the exception has a ResourceNotFoundException as the cause,
		// then we have an easy answer.
		Throwable cause = ex.getCause();
		if (cause instanceof ResourceNotFoundException) {
			String resourceName = ((ResourceNotFoundException) cause).getResourceName();
			if (resourceName != null) {
				ClassDescriptor classDesc = ClassDescriptor.fromResourceName(resourceName);
				return classDesc.toDottedClassName();
			}
		}
		
		if (ex.getMessage() == null) {
			return null;
		}

		// Try the regular expression patterns to parse the class name
		// from the exception message.
		for (Pattern pattern : patternList) {
			Matcher matcher = pattern.matcher(ex.getMessage());
			if (matcher.matches())
				return matcher.group(1);
		}
		return null;
	}

}

// vim:ts=4
