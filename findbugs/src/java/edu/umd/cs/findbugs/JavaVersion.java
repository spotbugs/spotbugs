/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2005 University of Maryland
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support for finding out what version of Java we're running on.
 */
public class JavaVersion {
	private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(\\..*)?$");

	private final int major;
	private final int minor;
	private final String rest;

	/**
	 * Constant for the Java version we're currently running on.
	 */
	private static JavaVersion runtimeVersion;

	static {
		try {
			runtimeVersion = new JavaVersion(SystemProperties.getProperty("java.version"));
		} catch (JavaVersionException e) {
			System.err.println("Warning: Unknown version of Java");
			// Assume Java 1.0.
			runtimeVersion = new JavaVersion(1, 0);
			e.printStackTrace();
		}
	}

	/**
	 * Constant for Java 1.5 (Tiger).
	 */
	public static final JavaVersion JAVA_1_5 = new JavaVersion(1, 5);

	/**
	 * Constructor.
	 *
	 * @param versionString a version string, as returned from the
	 *                      <code>java.version</code> system property:
	 *                      e.g., "1.4.2_04"
	 */
	public JavaVersion(String versionString) throws JavaVersionException {
		Matcher matcher = PATTERN.matcher(versionString);
		if (!matcher.matches())
			throw new JavaVersionException("Could not parse Java version string: " + versionString);
		try {
			major = Integer.parseInt(matcher.group(1));
			minor = Integer.parseInt(matcher.group(2));
			if (matcher.group(3) != null)
				rest = matcher.group(3);
			else
				rest = "";
		} catch (NumberFormatException e) {
			throw new JavaVersionException("Could not parse Java Version string: " + versionString, e);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param major major version
	 * @param minor minor version
	 */
	public JavaVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
		this.rest = "";
	}

	/**
	 * Get the major version number.
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Get the minor version number.
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Get the rest of the version string after the major and minor numbers.
	 */
	public String getRest() {
		return rest;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(major);
		buf.append('.');
		buf.append(minor);
		if (rest != null) {
			buf.append(rest);
		}
		
		return buf.toString();
	}

	/**
	 * Get the version of Java that we are currently
	 * running under.
	 */
	public static JavaVersion getRuntimeVersion() {
		return runtimeVersion;
	}

	/**
	 * Return whether the Java version represented by this
	 * object is at least as recent as the one given.
	 *
	 * @param other another JavaVersion
	 * @return true if this Java version is at least as recent as
	 *         the one given
	 */
	public boolean isSameOrNewerThan(JavaVersion other) {
		return this.major > other.major ||
		        (this.major == other.major && this.minor >= other.minor);
	}
}

// vim:ts=3
