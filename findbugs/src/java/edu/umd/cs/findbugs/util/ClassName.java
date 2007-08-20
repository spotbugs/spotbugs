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

package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Utility methods for working with class names.
 * 
 * @author David Hovemeyer
 */
public abstract class ClassName {
	
	public static String toSignature(@SlashedClassName String className) {
		if (className.charAt(0) == '[' || className.endsWith(";")) return className;
		return "L" + className + ";";
	}
	/**
	 * Convert class name to slashed format.
	 * If the class name is already in slashed format,
	 * it is returned unmodified.
	 * 
	 * @param className a class name
	 * @return the same class name in slashed format
	 */
	public static @SlashedClassName String toSlashedClassName(String className) {
		if (className.indexOf('.') >= 0) {
			return className.replace('.', '/');
		}
		return className;
	}

	/**
	 * Convert class name to dotted format.
	 * If the class name is already in dotted format,
	 * it is returned unmodified.
	 * 
	 * @param className a class name
	 * @return the same class name in dotted format
	 */
	public static String toDottedClassName(String className) {
		if (className.indexOf('/') >= 0) {
			className = className.replace('/', '.');
		}
		return className;
	}

	/**
	 * extract the package name from a dotted class name.
	 * Package names are always in dotted format.
	 * 
	 * @param className a dotted class name
	 * @return the name of the package containing the class
	 */
	public static @DottedClassName String extractPackageName(@DottedClassName String className) {
		int i = className.lastIndexOf('.');
		if (i < 0) return "";
		return className.substring(0, i);
	}

	/**
	 * Return whether or not the given class name is valid.
	 * 
	 * @param className a possible class name
	 * @return true if it's a valid class name, false otherwise
	 */
	public static boolean isValidClassName(String className) {
		// FIXME: should use a regex

		if (className.indexOf('(') >= 0) {
			return false;
		}
		return true;
	}

	/**
	 * Does a class name appear to designate an anonymous class?
	 * Only the name is analyzed. No classes are loaded or looked up.
	 *
	 * @param className  class name, slashed or dotted, fully qualified or unqualified
	 * @return true if className is the name of an anonymous class
	 */
	public static boolean isAnonymous(String className) {
		int i = className.lastIndexOf('$');
		if (i >= 0 && i + 1 < className.length()) {
			return Character.isDigit(className.charAt(i + 1));
		}
		return false;
	}

}
