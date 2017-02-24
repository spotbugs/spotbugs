/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008 University of Maryland
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

import java.io.File;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * Some utility methods for working with the Java class path.
 *
 * @author David Hovemeyer
 */
public class ClassPathUtil {
    /**
     * Try to find a codebase with the given name in the given class path
     * string.
     *
     * @param codeBaseName
     *            name of a codebase (e.g., "findbugs.jar")
     * @param classPath
     *            a classpath
     * @return full path of named codebase, or null if the codebase couldn't be
     *         found
     */
    public static String findCodeBaseInClassPath(@Nonnull String codeBaseName, String classPath) {
        if (classPath == null) {
            return null;
        }

        StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator);
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            File f = new File(t);
            if (f.getName().equals(codeBaseName)) {
                return t;
            }
        }

        return null;
    }

    /**
     * Try to find a codebase matching the given pattern in the given class path
     * string.
     *
     * @param codeBaseNamePattern
     *            pattern describing a codebase (e.g., compiled from the regex
     *            "findbugs\\.jar$")
     * @param classPath
     *            a classpath
     * @return full path of named codebase, or null if the codebase couldn't be
     *         found
     */
    public static String findCodeBaseInClassPath(Pattern codeBaseNamePattern, String classPath) {
        if (classPath == null) {
            return null;
        }

        StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator);
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            File f = new File(t);
            Matcher m = codeBaseNamePattern.matcher(f.getName());
            if (m.matches()) {
                return t;
            }
        }

        return null;
    }

    public static String[] getJavaClassPath() {
        return System.getProperty("java.class.path").split(":");
    }
}
