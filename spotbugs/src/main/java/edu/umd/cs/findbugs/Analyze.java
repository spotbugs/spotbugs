package edu.umd.cs.findbugs;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class Analyze {
    static private JavaClass serializable;

    static private JavaClass collection;

    static private JavaClass comparator;

    static private JavaClass map;

    static private JavaClass remote;

    static private ClassNotFoundException storedException;

    static {
        try {
            serializable = AnalysisContext.lookupSystemClass("java.io.Serializable");
            collection = AnalysisContext.lookupSystemClass("java.util.Collection");
            map = AnalysisContext.lookupSystemClass("java.util.Map");
            comparator = AnalysisContext.lookupSystemClass("java.util.Comparator");

        } catch (ClassNotFoundException e) {
            storedException = e;
        }
        try {
            remote = AnalysisContext.lookupSystemClass("java.rmi.Remote");
        } catch (ClassNotFoundException e) {
            if (storedException == null) {
                storedException = e;
            }
        }
    }

    /*
    private static boolean containsConcreteClasses(Set<JavaClass> s) {
        for (JavaClass c : s)
            if (!c.isInterface() && !c.isAbstract())
                return true;
        return false;
    }
     */

    public static double isDeepSerializable(String refSig) throws ClassNotFoundException {
        if (storedException != null) {
            throw storedException;
        }

        if (isPrimitiveComponentClass(refSig)) {
            return 1.0;
        }

        String refName = getComponentClass(refSig);
        if ("java.lang.Object".equals(refName)) {
            return 0.99;
        }

        JavaClass refJavaClass = Repository.lookupClass(refName);
        return isDeepSerializable(refJavaClass);
    }

    public static double isDeepRemote(String refSig) {
        if (remote == null) {
            return 0.1;
        }

        String refName = getComponentClass(refSig);
        if ("java.lang.Object".equals(refName)) {
            return 0.99;
        }

        JavaClass refJavaClass;
        try {
            refJavaClass = Repository.lookupClass(refName);
            return deepInstanceOf(refJavaClass, remote);
        } catch (ClassNotFoundException e) {
            return 0.99;
        }

    }

    private static boolean isPrimitiveComponentClass(String refSig) {
        int c = 0;
        while (c < refSig.length() && refSig.charAt(c) == '[') {
            c++;
        }

        // If the string is now empty, then we evidently have
        // an invalid type signature. We'll return "true",
        // which in turn will cause isDeepSerializable() to return
        // 1.0, hopefully avoiding any warnings from being generated
        // by whatever detector is calling us.
        return c >= refSig.length() || refSig.charAt(c) != 'L';
    }

    public static String getComponentClass(String refSig) {
        while (refSig.charAt(0) == '[') {
            refSig = refSig.substring(1);
        }

        // TODO: This method now returns primitive type signatures, is this ok?
        if (refSig.charAt(0) == 'L') {
            return refSig.substring(1, refSig.length() - 1).replace('/', '.');
        }
        return refSig;
    }

    public static double isDeepSerializable(JavaClass x) throws ClassNotFoundException {
        if (storedException != null) {
            throw storedException;
        }

        double result = deepInstanceOf(x, serializable);
        if (result >= 0.9) {
            return result;
        }
        result = Math.max(result, deepInstanceOf(x, collection));
        if (result >= 0.9) {
            return result;
        }
        result = Math.max(result, deepInstanceOf(x, map));
        if (result >= 0.9) {
            return result;
        }
        result = Math.max(result, 0.5 * deepInstanceOf(x, comparator));
        if (result >= 0.9) {
            return result;
        }
        return result;
    }

    /**
     * Given two JavaClasses, try to estimate the probability that an reference
     * of type x is also an instance of type y. Will return 0 only if it is
     * impossible and 1 only if it is guaranteed.
     *
     * @param x
     *            Known type of object
     * @param y
     *            Type queried about
     * @return 0 - 1 value indicating probablility
     */

    public static double deepInstanceOf(@DottedClassName String x, @DottedClassName String y) throws ClassNotFoundException {
        return deepInstanceOf(AnalysisContext.currentAnalysisContext().lookupClass(x), AnalysisContext.currentAnalysisContext()
                .lookupClass(y));
    }

    /**
     * Given two JavaClasses, try to estimate the probability that an reference
     * of type x is also an instance of type y. Will return 0 only if it is
     * impossible and 1 only if it is guaranteed.
     *
     * @param x
     *            Known type of object
     * @param y
     *            Type queried about
     * @return 0 - 1 value indicating probability
     */
    public static double deepInstanceOf(JavaClass x, JavaClass y) throws ClassNotFoundException {

        if (x.equals(y)) {
            return 1.0;
        }
        if ("java.lang.Object".equals(y.getClassName())) {
            return 1.0;
        }
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        ClassDescriptor xDesc = DescriptorFactory.createClassDescriptor(x);
        ClassDescriptor yDesc = DescriptorFactory.createClassDescriptor(y);

        boolean xIsSubtypeOfY = Repository.instanceOf(x, y);
        if (xIsSubtypeOfY) {
            return 1.0;
        }
        boolean yIsSubtypeOfX = Repository.instanceOf(y, x);
        if (!yIsSubtypeOfX) {
            if (x.isFinal() || y.isFinal()) {
                return 0.0;
            }
            if (!x.isInterface() && !y.isInterface()) {
                return 0.0;
            }
        }

        Set<ClassDescriptor> transitiveCommonSubtypes = subtypes2.getTransitiveCommonSubtypes(xDesc, yDesc);

        if (transitiveCommonSubtypes.isEmpty()) {
            for (ClassDescriptor c : subtypes2.getSubtypes(xDesc)) {
                XClass cx;
                try {
                    cx = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                } catch (CheckedAnalysisException e) {
                    continue;
                }
                if (!cx.isAbstract() && !cx.isInterface()) {
                    if (x.isAbstract() || x.isInterface()) {
                        return 0.2;
                    }
                    return 0.1;
                }
            }
            return 0.3;
        }

        // exist classes that are both X and Y
        Set<ClassDescriptor> xButNotY = new HashSet<ClassDescriptor>(subtypes2.getSubtypes(xDesc));
        xButNotY.removeAll(transitiveCommonSubtypes);
        for (ClassDescriptor c : xButNotY) {

            try {
                XClass cx = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                if (!cx.isAbstract() && !cx.isInterface()) {
                    return 0.7;
                }
            } catch (CheckedAnalysisException e) {
                continue;
            }

        }
        return 0.99;
    }
}
