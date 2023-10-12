/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2018, University of Maryland
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.annotation.CheckForNull;
import javax.annotation.meta.When;

/**
 * Utility methods for working with class names.
 *
 * @author David Hovemeyer
 */
public abstract class ClassName {

    public static boolean isMathClass(@SlashedClassName String className) {
        return "java/lang/Math".equals(className) || "java/lang/StrictMath".equals(className);
    }

    public static void assertIsDotted(@DottedClassName String className) {
        assert className.indexOf('/') == -1 : "Not dotted: " + className;
    }

    public static void assertIsSlashed(@SlashedClassName String className) {
        assert className.indexOf('.') == -1 : "Not slashed: " + className;
    }

    public static String toSignature(@SlashedClassName String className) {
        if (className.length() == 0) {
            throw new IllegalArgumentException("classname can't be empty");
        }
        if (className.charAt(0) == '[' || className.endsWith(";")) {
            return className;
        }
        return "L" + className + ";";
    }

    public static @CheckForNull String getPrimitiveType(@SlashedClassName String cls) {
        if (!cls.startsWith("java/lang/")) {
            return null;
        }
        cls = cls.substring(10);
        if ("Integer".equals(cls)) {
            return "I";
        }
        if ("Float".equals(cls)) {
            return "F";
        }
        if ("Double".equals(cls)) {
            return "D";
        }
        if ("Long".equals(cls)) {
            return "J";
        }
        if ("Byte".equals(cls)) {
            return "B";
        }
        if ("Character".equals(cls)) {
            return "C";
        }
        if ("Short".equals(cls)) {
            return "S";
        }
        if ("Boolean".equals(cls)) {
            return "Z";
        }
        return null;
    }

    /**
     * Converts from signature to slashed class name
     * (e.g., from Ljava/lang/String; to java/lang/String).
     * Returns null if it is the signature for an array or
     * primitive type.
     */
    public static @CheckForNull @SlashedClassName String fromFieldSignature(String signature) {
        if (signature.charAt(0) != 'L') {
            return null;
        }
        return signature.substring(1, signature.length() - 1);
    }

    /**
     * Convert class name to slashed format. If the class name is already in
     * slashed format, it is returned unmodified.
     *
     * @param className
     *            a class name
     * @return the same class name in slashed format
     */
    @SlashedClassName
    @SuppressFBWarnings("TQ_EXPLICIT_UNKNOWN_SOURCE_VALUE_REACHES_ALWAYS_SINK")
    public static String toSlashedClassName(@SlashedClassName(when = When.UNKNOWN) String className) {
        if (className.indexOf('.') >= 0) {
            return className.replace('.', '/');
        }
        return className;
    }

    /**
     * Convert class name to dotted format. If the class name is already in
     * dotted format, it is returned unmodified.
     *
     * @param className
     *            a class name
     * @return the same class name in dotted format
     */
    @DottedClassName
    @SuppressFBWarnings("TQ_EXPLICIT_UNKNOWN_SOURCE_VALUE_REACHES_NEVER_SINK")
    public static String toDottedClassName(@SlashedClassName(when = When.UNKNOWN) String className) {
        if (className.indexOf('/') >= 0) {
            return className.replace('/', '.');
        }
        return className;
    }

    /**
     * extract the package name from a dotted class name. Package names are
     * always in dotted format.
     *
     * @param className
     *            a dotted class name
     * @return the name of the package containing the class
     */
    public static @DottedClassName String extractPackageName(@DottedClassName String className) {
        int i = className.lastIndexOf('.');
        if (i < 0) {
            return "";
        }
        return className.substring(0, i);
    }

    public static String extractSimpleName(@DottedClassName String className) {
        int i = className.lastIndexOf('.');
        if (i > 0) {
            className = className.substring(i + 1);
        }
        // to be consistent with the Class.getSimpleName(),
        // simple class name does not! contain the enclosing class name
        i = className.lastIndexOf('$');
        if (i > 0) {
            className = className.substring(i + 1);
        }
        // can be empty!
        return className;
    }

    /**
     * Return whether or not the given class name is valid.
     *
     * @param className
     *            a possible class name
     * @return true if it's a valid class name, false otherwise
     */
    public static boolean isValidClassName(String className) {
        return !className.isEmpty() &&
                (isValidBinaryClassName(className) ||
                        isValidDottedClassName(className) ||
                        isValidArrayFieldDescriptor(className) ||
                        isValidClassFieldDescriptor(className));
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2.1">
     *     JVMS (Java SE 8 Edition) 4.2.1. Binary Class and Interface Names
     *     </a>
     */
    private static boolean isValidBinaryClassName(String className) {
        return className.indexOf('.') == -1 &&
                className.indexOf('[') == -1 &&
                className.indexOf(';') == -1;
    }

    private static boolean isValidDottedClassName(String className) {
        return className.indexOf('/') == -1 &&
                className.indexOf('[') == -1 &&
                className.indexOf(';') == -1;
    }

    /**
     * Determines whether a class name is a valid array field descriptor as per
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2">
     * JVMS (Java SE 8 Edition) 4.3.2</a>
     *
     * @param className a class name to test for validity - must be non-{@code null} and non-empty.
     * @return {@code true} if {@code className} is a valid array field descriptor as
     *          per JVMS 4.3.2, otherwise {@code false}
     * @throws IndexOutOfBoundsException if {@code className} is empty.
     * @throws NullPointerException if {@code className} is {@code null}.
     */
    private static boolean isValidArrayFieldDescriptor(String className) {
        String tail = className.substring(1);
        return className.startsWith("[") &&
                (isValidArrayFieldDescriptor(tail) ||
                        isValidClassFieldDescriptor(tail) ||
                        isValidBaseTypeFieldDescriptor(tail));

    }

    private static boolean isValidClassFieldDescriptor(String className) {
        return className.startsWith("L") &&
                className.endsWith(";") &&
                isValidBinaryClassName(className.substring(1, className.length() - 1));
    }

    private static boolean isValidBaseTypeFieldDescriptor(String className) {
        return "B".equals(className) ||
                "C".equals(className) ||
                "D".equals(className) ||
                "F".equals(className) ||
                "I".equals(className) ||
                "J".equals(className) ||
                "S".equals(className) ||
                "Z".equals(className);
    }

    /**
     * Does a class name appear to designate an anonymous or local (defined
     * inside method) class? Only the name is analyzed. No classes are loaded or
     * looked up.
     *
     * @param className
     *            class name, slashed or dotted, fully qualified or unqualified
     * @return true if className is the name of an anonymous or local class
     */
    public static boolean isLocalOrAnonymous(String className) {
        int i = className.lastIndexOf('$');
        if (i >= 0 && i + 1 < className.length()) {
            return Character.isDigit(className.charAt(i + 1));
        }
        return false;
    }

    /**
     * Does a class name appear to designate an anonymous class? Only
     * the name is analyzed. No classes are loaded or looked up.
     *
     * @param className
     *            class name, slashed or dotted, fully qualified or unqualified
     * @return true if className is the name of an anonymous class
     */
    public static boolean isAnonymous(String className) {
        int i = className.lastIndexOf('$');
        if (i >= 0 && ++i < className.length()) {
            while (i < className.length()) {
                if (!Character.isDigit(className.charAt(i))) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }

    /**
     * Extract a slashed classname from a JVM classname or signature.
     *
     * @param originalName
     *            JVM classname or signature
     * @return a slashed classname
     */
    public static @SlashedClassName String extractClassName(String originalName) {
        String name = originalName;
        if (name.charAt(0) != '[' && name.charAt(name.length() - 1) != ';') {
            return name;
        }
        while (name.charAt(0) == '[') {
            name = name.substring(1);
        }
        if (name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';') {
            name = name.substring(1, name.length() - 1);
        }
        if (name.charAt(0) == '[') {
            throw new IllegalArgumentException("Bad class name: " + originalName);
        }
        return name;
    }

    public static String extractPackagePrefix(String packageName, int count) {
        int dotsSeen = 0;
        int prefixLength = 0;
        while (dotsSeen < count) {
            int p = packageName.indexOf('.', prefixLength);
            if (p < 0) {
                return packageName;
            }
            prefixLength = p + 1;
            dotsSeen++;
        }
        if (prefixLength == 0) {
            return "";
        }
        return packageName.substring(0, prefixLength - 1);
    }

    public static boolean matchedPrefixes(String[] classSearchStrings, @DottedClassName String className) {
        String[] pp = classSearchStrings;
        if (pp == null || pp.length == 0) {
            return true;
        }

        for (String p : pp) {
            if (p.length() > 0 && (StringUtils.containsIgnoreCase(className, p) || fuzzyMatch(className, p))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Perform a fuzzy matching, by comparing the Levenshtein distance of the
     * simple class name and the search string. A maximum distance of 3 is used.
     * This means the searchString and the className may differ by 3 single-character
     * edits (insertions, deletions or substitutions). This limit also speeds up the computation.
     * <p>
     * For more information on the Levenshtein distance see
     * <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Wikipedia</a> and the
     * <a href="https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/LevenshteinDistance.html">Apache Commons Text JavaDoc</a>.
     *
     * @param className    the full class name
     * @param searchString the search string
     * @return true, if the strings are similar, false otherwise
     */
    private static boolean fuzzyMatch(String className, String searchString) {
        // compare to a maximum Levenshtein distance of 3
        LevenshteinDistance ld = new LevenshteinDistance(3);
        return ld.apply(extractSimpleName(className), searchString) != -1;
    }


    public static @SlashedClassName String toSlashedClassName(Class<?> class1) {
        return toSlashedClassName(class1.getName());
    }
}
