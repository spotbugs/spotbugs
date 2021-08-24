package edu.umd.cs.findbugs.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.AnalysisContext;

public class MutableClasses {

    private static final Set<String> KNOWN_IMMUTABLE_CLASSES = new HashSet<>(Arrays.asList(
            "java.lang.String", "java.lang.Integer", "java.lang.Byte", "java.lang.Character",
            "java.lang.Short", "java.lang.Boolean", "java.lang.Long", "java.lang.Double",
            "java.lang.Float", "java.lang.StackTraceElement", "java.math.BigInteger",
            "java.math.Decimal", "java.io.File", "java.awt.Font", "java.awt.BasicStroke",
            "java.awt.Color", "java.awt.GradientPaint", "java.awt.LinearGradientPaint",
            "java.awt.RadialGradientPaint", "java.Cursor.", "java.util.UUID", "java.net.URL",
            "java.net.URI", "java.net.Inet4Address", "java.net.Inet6Address", "java.net.InetSocketAddress",
            "java.security.Permission"));

    private static final List<String> SETTER_LIKE_NAMES = Arrays.asList(
            "set", "put", "add", "insert", "delete", "remove", "erase", "clear", "push", "pop",
            "enqueue", "dequeue", "write", "append", "replace");

    public static boolean mutableSignature(String sig) {
        if (sig.charAt(0) == '[') {
            return true;
        }

        if (sig.charAt(0) != 'L') {
            return false;
        }

        String dottedClassName = sig.substring(1, sig.length() - 1).replace('/', '.');
        if (KNOWN_IMMUTABLE_CLASSES.contains(dottedClassName)) {
            return false;
        }

        try {
            JavaClass cls = Repository.lookupClass(dottedClassName);
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
        for (String name : SETTER_LIKE_NAMES) {
            if (method.getName().startsWith(name)) {
                String retSig = method.getReturnType().getSignature();
                // If setter-like methods returns an object of the same type then we suppose that it
                // is not a setter but creates a new instance instead.
                return !("L" + cls.getClassName().replace('.', '/') + ";").equals(retSig);
            }
        }
        return false;
    }
}
