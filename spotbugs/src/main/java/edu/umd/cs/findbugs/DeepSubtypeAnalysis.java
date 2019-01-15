package edu.umd.cs.findbugs;

import java.util.List;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class DeepSubtypeAnalysis {
    static private JavaClass serializable;

    static private JavaClass collection;

    static private JavaClass comparator;

    static private JavaClass map;

    static private JavaClass remote;

    static private ClassNotFoundException storedException;

    private static final boolean DEBUG = SystemProperties.getBoolean("dsa.debug");

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

    public static double isDeepSerializable(ReferenceType type) throws ClassNotFoundException {
        if (type instanceof ArrayType) {
            ArrayType a = (ArrayType) type;
            Type t = a.getBasicType();
            if (t instanceof ReferenceType) {
                type = (ReferenceType) t;
            } else {
                return 1.0;
            }
        }
        double result =  isDeepSerializable(type.getSignature());
        if (type instanceof GenericObjectType && Subtypes2.isContainer(type)) {
            GenericObjectType gt = (GenericObjectType) type;
            List<? extends ReferenceType> parameters = gt.getParameters();
            if (parameters != null) {
                for(ReferenceType t : parameters) {
                    double r = isDeepSerializable(t);
                    if (result > r) {
                        result = r;
                    }
                }
            }
        }

        return result;
    }
    public static ReferenceType getLeastSerializableTypeComponent(ReferenceType type)
            throws ClassNotFoundException {
        if (type instanceof ArrayType) {
            ArrayType a = (ArrayType) type;
            Type t = a.getBasicType();
            if (t instanceof ReferenceType) {
                type = (ReferenceType) t;
            } else {
                return type;
            }
        }

        ReferenceType result = type;
        double value =  isDeepSerializable(type.getSignature());
        if (type instanceof GenericObjectType && Subtypes2.isContainer(type)) {
            GenericObjectType gt = (GenericObjectType) type;
            List<? extends ReferenceType> parameters = gt.getParameters();
            if (parameters != null) {
                for(ReferenceType t : parameters) {
                    double r = isDeepSerializable(t);
                    if (value > r) {
                        value = r;
                        result = getLeastSerializableTypeComponent(t);
                    }
                }
            }
        }

        return result;
    }

    public static double isDeepSerializable(@DottedClassName String refSig) throws ClassNotFoundException {
        if (storedException != null) {
            throw storedException;
        }

        if (isPrimitiveComponentClass(refSig)) {
            if (DEBUG) {
                System.out.println("regSig \"" + refSig + "\" is primitive component class");
            }
            return 1.0;
        }

        String refName = getComponentClass(refSig);
        if ("java.lang.Object".equals(refName)) {
            return 0.99;
        }

        JavaClass refJavaClass = Repository.lookupClass(refName);
        return isDeepSerializable(refJavaClass);
    }

    public static double isDeepRemote(ReferenceType refType) {
        return isDeepRemote(refType.getSignature());
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
            return Analyze.deepInstanceOf(refJavaClass, remote);
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

    public static String getComponentClass(ReferenceType refType) {
        return getComponentClass(refType.getSignature());
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

        if ("java.lang.Object".equals(x.getClassName())) {
            return 0.4;
        }

        if (DEBUG) {
            System.out.println("checking " + x.getClassName());
        }

        double result = Analyze.deepInstanceOf(x, serializable);
        if (result >= 0.9) {
            if (DEBUG) {
                System.out.println("Direct high serializable result: " + result);
            }
            return result;
        }

        if (x.isFinal()) {
            return result;
        }

        double collectionResult = Analyze.deepInstanceOf(x, collection);
        double mapResult = Analyze.deepInstanceOf(x, map);

        if (x.isInterface() || x.isAbstract()) {
            result = Math.max(result, Math.max(mapResult, collectionResult) * 0.95);
            if (result >= 0.9) {
                return result;
            }
        }
        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor(x);

        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

        Set<ClassDescriptor> directSubtypes = subtypes2.getDirectSubtypes(classDescriptor);
        directSubtypes.remove(classDescriptor);


        double confidence = 0.6;
        if (x.isAbstract() || x.isInterface()) {
            confidence = 0.8;
            result = Math.max(result, 0.4);
        } else if (directSubtypes.isEmpty()) {
            confidence = 0.2;
        }

        double confidence2 = (1 + confidence) / 2;
        result = Math.max(result, confidence2 * collectionResult);
        if (result >= 0.9) {
            if (DEBUG) {
                System.out.println("High collection result: " + result);
            }
            return result;
        }
        result = Math.max(result, confidence2 * mapResult);
        if (result >= 0.9) {
            if (DEBUG) {
                System.out.println("High map result: " + result);
            }
            return result;
        }
        result = Math.max(result, confidence2 * 0.5 * Analyze.deepInstanceOf(x, comparator));
        if (result >= 0.9) {
            if (DEBUG) {
                System.out.println("High comparator result: " + result);
            }
            return result;
        }



        for (ClassDescriptor subtype : directSubtypes) {
            JavaClass subJavaClass = Repository.lookupClass(subtype.getDottedClassName());
            result = Math.max(result, confidence * Analyze.deepInstanceOf(subJavaClass, serializable));

            // result = Math.max(result, confidence * isDeepSerializable(subJavaClass));
            if (result >= 0.9) {
                return result;
            }
        }


        if (DEBUG) {
            System.out.println("No high results; max: " + result);
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
     * @return 0 - 1 value indicating probability
     */

    public static double deepInstanceOf(@DottedClassName String x, @DottedClassName String y) throws ClassNotFoundException {
        return Analyze.deepInstanceOf(x, y);
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
        return Analyze.deepInstanceOf(x, y);

    }
}
