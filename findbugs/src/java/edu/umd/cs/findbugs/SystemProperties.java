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

package edu.umd.cs.findbugs;

/**
 * @author pugh
 */
public class SystemProperties {

	public final static boolean ASSERTIONS_ENABLED;
	static {
		boolean tmp = false;
		assert(tmp = true);
		ASSERTIONS_ENABLED = tmp;
	}
	/**
	 * Get boolean property, returning false if a security manager prevents us
	 * from accessing system properties
	 * @return
	 */
	public static boolean getBoolean(String arg0) {
		try {
		return Boolean.getBoolean(arg0);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean getBoolean(String name, boolean defaultValue) {
		boolean result = defaultValue;
		try {
			String value = System.getProperty(name);
			if (value == null) return defaultValue;
			result = toBoolean(value);
		} catch (IllegalArgumentException e) {
		} catch (NullPointerException e) {
		}
		return result;
	}
	private static boolean toBoolean(String name) {
		return ((name != null) && name.equalsIgnoreCase("true"));
	}

	  
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public static Integer getInteger(String arg0, int arg1) {
		try {
				return Integer.getInteger(arg0, arg1);
		} catch (Exception e) {
			return arg1;
		}
	}

	/**
	 * @param arg0
	 * @return
	 */
	public static String getProperty(String arg0) {
		try {
		return System.getProperty(arg0);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public static String getProperty(String arg0, String arg1) {
		try {
		return System.getProperty(arg0, arg1);
		} catch (Exception e) {
			return arg1;
		}
	}

}
