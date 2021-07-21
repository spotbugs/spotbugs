package edu.umd.cs.findbugs.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;

public class MutableClasses {

    private static final Set<String> KNOWN_IMMUTABLE_CLASSES = new HashSet<>(Arrays.asList(
            "java.lang.String", "java.lang.Integer", "java.lang.Byte", "java.lang.Character",
            "java.lang.Short", "java.lang.Boolean", "java.lang.Long", "java.lang.Double",
            "java.lang.Float", "java.lang.StackTraceElement", "java.math.BigInteger",
            "java.math.Decimal", "java.io.File", "java.awt.Font", "java.awt.BasicStroke",
            "java.awt.Color", "java.awt.GradientPaint", "java.awt.LinearGradientPaint",
            "java.awt.RadialGradientPaint", "java.Cursor.", "java.util.UUID", "java.util.URL",
            "java.util.URI", "java.util.Inet4Address", "java.util.InetSocketAddress",
            "java.security.Permission", "com.google.common.collect.ImmutableBiMap",
            "com.google.common.collect.ImmutableClassToInstanceMap",
            "com.google.common.collect.ImmutableCollection",
            "com.google.common.collect.ImmutableList",
            "com.google.common.collect.ImmutableListMultimap",
            "com.google.common.collect.ImmutableMap",
            "com.google.common.collect.ImmutableMultimap",
            "com.google.common.collect.ImmutableMultiset",
            "com.google.common.collect.ImmutableRangeMap",
            "com.google.common.collect.ImmutableRangeSet",
            "com.google.common.collect.ImmutableSet",
            "com.google.common.collect.ImmutableSetMultimap",
            "com.google.common.collect.ImmutableSortedMap",
            "com.google.common.collect.ImmutableSortedMultiset",
            "com.google.common.collect.ImmutableSortedSet",
            "com.google.common.collect.ImmutableTable"));

    private static final Set<String> KNOWN_IMMUTABLE_PACKAGES = new HashSet<>(Arrays.asList(
            "java.math", "java.time"));

    private static final List<String> SETTER_LIKE_NAMES = Arrays.asList(
            "set", "put", "add", "insert", "delete", "remove", "erase", "clear", "push", "pop",
            "enqueue", "dequeue", "append", "replace");

    private static Set<String> userDefinedImmutableClasses;
    private static Set<String> userDefinedImmutablePackages;
    static {
        String envClsString = System.getenv("spotbugs.immutableclasses");
        if (envClsString == null) {
            envClsString = System.getenv("spotbugs_immutableclasses");
        }

        envClsString = SystemProperties.getProperty("spotbugs.immutableclasses", envClsString);
        if (envClsString != null) {
            List<String> clsList = Arrays.asList(envClsString.split(";"));
            for (String clsName: clsList) {
                if (clsName.endsWith(".*")) {
                    if (userDefinedImmutablePackages == null) {
                        userDefinedImmutablePackages = new HashSet<>();
                    }
                    userDefinedImmutablePackages.add(clsName.substring(0, clsName.length() - 2));
                } else {
                    if (userDefinedImmutableClasses == null) {
                        userDefinedImmutableClasses = new HashSet<>();
                    }
                    userDefinedImmutableClasses.add(clsName);
                }
            }
        }
    }

    public static boolean mutableSignature(String sig) {
        if (sig.charAt(0) == '[') {
            return true;
        }

        if (sig.charAt(0) != 'L') {
            return false;
        }

        String dottedClassName = sig.substring(1, sig.length() - 1).replace('/', '.');
        int lastDot = dottedClassName.lastIndexOf('.');

        if (lastDot >= 0) {
            String dottedPackageName = dottedClassName.substring(0, lastDot);

            if (KNOWN_IMMUTABLE_PACKAGES.contains(dottedPackageName)) {
                return false;
            }

            if (userDefinedImmutablePackages != null && userDefinedImmutablePackages.contains(dottedPackageName)) {
                return false;
            }
        }

        if (KNOWN_IMMUTABLE_CLASSES.contains(dottedClassName)) {
            return false;
        }

        if (userDefinedImmutableClasses != null && userDefinedImmutableClasses.contains(dottedClassName)) {
            return false;
        }

        try {
            JavaClass cls = Repository.lookupClass(dottedClassName);
            if (Stream.of(cls.getAnnotationEntries()).anyMatch(s -> (s.getAnnotationType().endsWith(".Immutable"))
                    || s.getAnnotationType().equals("jdk.internal.ValueBased"))) {
                return false;
            }
            return isMutable(cls);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    private static boolean isMutable(JavaClass cls) {
        for (Method method : cls.getMethods()) {
            if (looksLikeASetter(method, cls)) {
                return true;
            }
        }
        try {
            JavaClass sup = cls.getSuperClass();
            if (sup != null) {
                return isMutable(sup);
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return false;
    }

    public static boolean looksLikeASetter(Method method, JavaClass cls) {
        // Static methods are no setters.
        if (method.isStatic()) {
            return false;
        }

        // Non-public setters cannot be used from outside the package so disregard them.
        if (!method.isPublic()) {
            return false;
        }

        // If the method returns an object then we suppose that it
        // is not a setter but creates a new instance instead.
        if (method.getReturnType().getSignature().startsWith("L")) {
            return false;
        }

        // If the method throws an UnsupportedOperationException
        // then we must ignore it.
        ExceptionTable exceptions = method.getExceptionTable();
        if (exceptions != null) {
            if (Arrays.asList(exceptions.getExceptionNames()).contains("java.lang.UnsupportedOperationException")) {
                return false;
            }
        }

        for (String name : SETTER_LIKE_NAMES) {
            if (method.getName().startsWith(name)) {
                return true;
            }
        }
        return false;
    }
}
