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

/**
 * Support for finding out what version of Java we're running on.
 */
public class JavaVersion {

    private final int major;

    private final int minor;

    private final String rest;

    /**
     * StaticConstant for the Java version we're currently running on.
     */
    private static JavaVersion runtimeVersion;

    static {
        try {
            runtimeVersion = new JavaVersion(SystemProperties.getProperty("java.version"));
        } catch (JavaVersionException e) {
            // Assume Java 1.8.
            runtimeVersion = new JavaVersion(1, 8);
            e.printStackTrace();
        }
    }

    /**
     * StaticConstant for Java 1.5 (Tiger).
     */
    public static final JavaVersion JAVA_1_5 = new JavaVersion(1, 5);

    /**
     * Constructor.
     *
     * @param versionString
     *            a version string, as returned from the
     *            <code>java.version</code> system property: e.g., "1.4.2_04"
     */
    public JavaVersion(String versionString) throws JavaVersionException {
        // See https://bugs.openjdk.java.net/browse/JDK-8061493 for http://openjdk.java.net/jeps/223
        // The only common between pre-9 and 9+ are the dots as separators...
        String[] strings = versionString.split("\\.", 3);
        try {
            // We need only care about 9+ early access in first segment
            int earlyAccess = strings[0].indexOf('-');
            if (earlyAccess > 0) {
                // 9+ early access versions do not follow common scheme above (it would be too easy)
                // they look like 9-ea+73 and have only one meaningful (major) part for us.
                major = Integer.parseInt(strings[0].substring(0, earlyAccess));
                minor = 0;
                rest = strings[0].substring(earlyAccess);
            } else {
                major = Integer.parseInt(strings[0]);
                if (strings.length > 1) {
                    minor = Integer.parseInt(strings[1]);
                    if (strings.length > 2) {
                        rest = strings[2];
                    } else {
                        rest = "";
                    }
                } else {
                    minor = 0;
                    rest = "";
                }
            }
        } catch (NumberFormatException e) {
            throw new JavaVersionException("Could not parse Java Version string: " + versionString, e);
        }
    }

    /**
     * Constructor.
     *
     * @param major
     *            major version
     * @param minor
     *            minor version
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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(major);
        buf.append('.');
        buf.append(minor);
        if (rest != null) {
            buf.append(rest);
        }

        return buf.toString();
    }

    /**
     * Get the version of Java that we are currently running under.
     */
    public static JavaVersion getRuntimeVersion() {
        return runtimeVersion;
    }

    /**
     * Return whether the Java version represented by this object is at least as
     * recent as the one given.
     *
     * @param other
     *            another JavaVersion
     * @return true if this Java version is at least as recent as the one given
     */
    public boolean isSameOrNewerThan(JavaVersion other) {
        return this.major > other.major || (this.major == other.major && this.minor >= other.minor);
    }
}
