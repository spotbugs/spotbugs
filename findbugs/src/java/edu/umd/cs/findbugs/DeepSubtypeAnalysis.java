package edu.umd.cs.findbugs;

import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class DeepSubtypeAnalysis {
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
			if (storedException == null) storedException = e;
		}
	}

	private static boolean containsConcreteClasses(Set<JavaClass> s) {
		for (JavaClass c : s)
			if (!c.isInterface() && !c.isAbstract())
				return true;
		return false;
	}

	public static double isDeepSerializable(String refSig)
			throws ClassNotFoundException {
		if (storedException != null)
			throw storedException;

		if (isPrimitiveComponentClass(refSig))
			return 1.0;

		String refName = getComponentClass(refSig);
		if (refName.equals("java.lang.Object"))
			return 0.99;

		JavaClass refJavaClass = Repository.lookupClass(refName);
		return isDeepSerializable(refJavaClass);
	}

	public static double isDeepRemote(String refSig) {
		if (remote == null) return 0.1;

		String refName = getComponentClass(refSig);
		if (refName.equals("java.lang.Object"))
			return 0.99;

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
		// an invalid type signature.  We'll return "true",
		// which in turn will cause isDeepSerializable() to return
		// 1.0, hopefully avoiding any warnings from being generated
		// by whatever detector is calling us.
		return c >= refSig.length() || refSig.charAt(c) != 'L';
	}

	public static String getComponentClass(String refSig) {
		while (refSig.charAt(0) == '[')
			refSig = refSig.substring(1);

		//TODO: This method now returns primitive type signatures, is this ok?
		if (refSig.charAt(0) == 'L')
			return refSig.substring(1, refSig.length() - 1).replace('/', '.');
		return refSig;
	}

	public static double isDeepSerializable(JavaClass x)
			throws ClassNotFoundException {
		if (storedException != null)
			throw storedException;

		if (x.getClassName().equals("java.lang.Object"))
			return 0.4;
		double result = Analyze.deepInstanceOf(x, serializable);
		if (result >= 0.9)
			return result;
		result = Math.max(result, Analyze.deepInstanceOf(x, collection));
		if (result >= 0.9)
			return result;
		result = Math.max(result, Analyze.deepInstanceOf(x, map));
		if (result >= 0.9)
			return result;
		result = Math.max(result, 0.5*Analyze.deepInstanceOf(x, comparator));
		if (result >= 0.9)
			return result;
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

	public static double deepInstanceOf(@DottedClassName String x, @DottedClassName String y)
	throws ClassNotFoundException {
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
	public static double deepInstanceOf(JavaClass x, JavaClass y)
			throws ClassNotFoundException {
		return Analyze.deepInstanceOf(x, y);

	}
}
