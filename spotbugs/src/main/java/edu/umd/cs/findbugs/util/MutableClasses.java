package edu.umd.cs.findbugs.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.AnalysisContext;

public class MutableClasses {

    private static final Set<String> KNOWN_IMMUTABLE_CLASSES = new HashSet<>(Arrays.asList(
            "java.lang.String", "java.lang.Integer", "java.lang.Byte", "java.lang.Character",
            "java.lang.Short", "java.lang.Boolean", "java.lang.Long", "java.lang.Double",
            "java.lang.Float", "java.lang.StackTraceElement", "java.lang.Class", "java.math.BigInteger",
            "java.math.Decimal", "java.io.File", "java.awt.Font", "java.awt.BasicStroke",
            "java.awt.Color", "java.awt.GradientPaint", "java.awt.LinearGradientPaint",
            "java.awt.RadialGradientPaint", "java.awt.Cursor", "java.util.Locale", "java.util.UUID", "java.net.URL",
            "java.net.URI", "java.net.Inet4Address", "java.net.Inet6Address", "java.net.InetSocketAddress",
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
        }

        if (KNOWN_IMMUTABLE_CLASSES.contains(dottedClassName)) {
            return false;
        }

        try {
            JavaClass cls = Repository.lookupClass(dottedClassName);
            if (Stream.of(cls.getAnnotationEntries()).map(AnnotationEntry::getAnnotationType)
                    .anyMatch(type -> type.endsWith("/Immutable;") || type.equals("Ljdk/internal/ValueBased;"))) {
                return false;
            }
            return ClassAnalysis.load(cls, sig).isMutable();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    /**
     * Analytic information about a {@link JavaClass} relevant to determining its mutability properties.
     */
    private static final class ClassAnalysis {
        private static final MethodChecker[] METHOD_CHECKERS = {
            new MethodChecker("add"),
            new MethodChecker("append"),
            new MethodChecker("clear"),
            new MethodChecker("delete"),
            new MethodChecker("enqueue"),
            new MethodChecker("erase"),
            new MethodChecker("insert"),
            new MethodChecker("pop"),
            new MethodChecker("push"),
            new MethodChecker("put"),
            new MethodChecker("remove"),
            new MethodChecker("replace"),
            new MethodChecker("set"),
            new MethodChecker("dequeue"),
            new WriteMethodChecker()
        };

        /**
         * Class under analysis.
         */
        private final JavaClass cls;
        /**
         * Superclass {@link ClassAnalysis}, lazily instantiated if present, otherwise {@code null}.
         */
        private ClassAnalysis superAnalysis;

        // Various lazily-determined properties of this class. null indicates the property has not been determined yet.
        private String sig;
        private Boolean mutable;
        private Boolean immutableByContract;
        private Boolean externalizable;
        private Boolean serializable;

        private ClassAnalysis(JavaClass cls, String sig) {
            this.cls = cls;
            this.sig = sig;
        }

        static ClassAnalysis load(JavaClass cls, String sig) {
            // TODO: is there a place where we can maintain a cache of these for the duration of analysis?
            return new ClassAnalysis(cls, sig);
        }

        boolean isMutable() {
            Boolean local = mutable;
            if (local == null) {
                mutable = local = computeMutable();
            }
            return local;
        }

        private boolean computeMutable() {
            if (isImmutableByContract()) {
                return false;
            }

            for (Method method : cls.getMethods()) {
                if (!method.isStatic() && looksLikeASetter(method)) {
                    return true;
                }
            }

            final ClassAnalysis maybeSuper = getSuperAnalysis();
            return maybeSuper != null && maybeSuper.isMutable();
        }

        boolean isExternalizable() {
            Boolean local = externalizable;
            if (local == null) {
                externalizable = local = computeExternalizable();
            }
            return local;
        }

        private boolean computeExternalizable() {
            // We are considering directly-implemented interfaces for now
            for (String iface : cls.getInterfaceNames()) {
                if (iface.equals("java.io.Externalizable")) {
                    return true;
                }
            }

            final ClassAnalysis maybeSuper = getSuperAnalysis();
            return maybeSuper != null && maybeSuper.isExternalizable();
        }

        boolean isSerializable() {
            Boolean local = serializable;
            if (local == null) {
                // Externalizable implies Serializable
                serializable = local = computeSerializable() || isExternalizable();
            }
            return local;
        }

        private boolean computeSerializable() {
            // We are considering directly-implemented interfaces for now
            for (String iface : cls.getInterfaceNames()) {
                if (iface.equals("java.io.Serializable")) {
                    return true;
                }
            }

            final ClassAnalysis maybeSuper = getSuperAnalysis();
            return maybeSuper != null && maybeSuper.isSerializable();
        }

        private boolean looksLikeASetter(Method method) {
            for (MethodChecker checker : METHOD_CHECKERS) {
                if (checker.isMutableMethod(this, method)) {
                    return true;
                }
            }
            return false;
        }

        String getSig() {
            String local = sig;
            if (local == null) {
                sig = local = "L" + cls.getClassName().replace('.', '/') + ";";
            }
            return local;
        }

        private boolean isImmutableByContract() {
            Boolean local = immutableByContract;
            if (local == null) {
                immutableByContract = local = computeByImmutableContract();
            }
            return local;
        }

        private boolean computeByImmutableContract() {
            for (AnnotationEntry entry : cls.getAnnotationEntries()) {
                // Error-Prone's @Immutable annotation is @Inherited, hence it applies to subclasses as well
                if (entry.getAnnotationType().equals("Lcom/google/errorprone/annotations/Immutable;")) {
                    return true;
                }
            }

            final ClassAnalysis maybeSuper = getSuperAnalysis();
            return maybeSuper != null && maybeSuper.isImmutableByContract();
        }

        private ClassAnalysis getSuperAnalysis() {
            ClassAnalysis local = superAnalysis;
            if (local == null) {
                superAnalysis = local = loadSuperAnalysis();
            }
            return local;
        }

        private ClassAnalysis loadSuperAnalysis() {
            // Quick check if there is a superclass of importance
            final String superName = cls.getSuperclassName();
            if (superName == null || superName.equals("java.lang.Object")) {
                return null;
            }

            final JavaClass superClass;
            try {
                superClass = cls.getSuperClass();
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
                return null;
            }
            if (superClass == null) {
                return null;
            }

            return load(superClass, null);
        }
    }

    private static class MethodChecker {
        private final String prefix;

        MethodChecker(String prefix) {
            this.prefix = prefix;
        }

        boolean isMutableMethod(ClassAnalysis cls, Method method) {
            return method.getName().startsWith(prefix)
                    // If setter-like methods returns an object of the same type then we suppose that it
                    // is not a setter but creates a new instance instead.
                    && !cls.getSig().equals(method.getReturnType().getSignature());
        }
    }

    private static final class WriteMethodChecker extends MethodChecker {
        WriteMethodChecker() {
            super("write");
        }

        @Override
        boolean isMutableMethod(ClassAnalysis cls, Method method) {
            if (!super.isMutableMethod(cls, method)) {
                return false;
            }

            final String name = method.getName();
            final String sig = method.getSignature();

            // writeReplace() is a special case for java.io.Serializable contract
            if (name.equals("writeReplace") && sig.equals("()Ljava/lang/Object") && cls.isSerializable()) {
                return false;
            }
            // writeExternal(ObjectOutput) is part of the java.io.Externalizable contract
            if (name.equals("writeExternal") && method.isPublic()
                    && sig.equals("(Ljava/io/ObjectOutput;)V") && cls.isExternalizable()) {
                return false;
            }

            return true;
        }
    }
}
