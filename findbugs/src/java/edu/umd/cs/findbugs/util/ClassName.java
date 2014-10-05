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

import javax.annotation.CheckForNull;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Utility methods for working with class names.
 *
 * @author David Hovemeyer
 */
public abstract class ClassName {

    public static boolean isMathClass(@SlashedClassName String className) {
        return "java/lang/Math".equals(className) || "java/lang/StrictMath".equals(className);
    }

    public static @DottedClassName String assertIsDotted(@DottedClassName String className) {
        assert className.indexOf('/') == -1 : "Not dotted: " + className;
        return className;
    }
    public static @SlashedClassName String assertIsSlashed(@SlashedClassName String className) {
        assert className.indexOf('.') == -1 : "Not slashed: " + className;
        return className;
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
    public static @CheckForNull
    @SlashedClassName
    String fromFieldSignature(String signature) {
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
            return DescriptorFactory.canonicalizeString(className.replace('.', '/'));
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
            return DescriptorFactory.canonicalizeString(className.replace('/', '.'));
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
    public static @DottedClassName
    String extractPackageName(@DottedClassName String className) {
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
        // FIXME: should use a regex

        if (className.indexOf('(') >= 0) {
            return false;
        }
        return true;
    }

    /**
     * Does a class name appear to designate an anonymous class? Only the name
     * is analyzed. No classes are loaded or looked up.
     *
     * @param className
     *            class name, slashed or dotted, fully qualified or unqualified
     * @return true if className is the name of an anonymous class
     */
    public static boolean isAnonymous(String className) {
        int i = className.lastIndexOf('$');
        if (i >= 0 && i + 1 < className.length()) {
            return Character.isDigit(className.charAt(i + 1));
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
    public static @SlashedClassName
    String extractClassName(String originalName) {
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
            if (p.length() > 0 && className.indexOf(p) >= 0) {
                return true;
            }
        }

        return false;

    }


    public static @SlashedClassName String toSlashedClassName(Class<?> class1) {
        return toSlashedClassName(class1.getName());
    }
}
