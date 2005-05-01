package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes;

public class Analyze {
	static private JavaClass serializable;

	static private JavaClass collection;

	static private JavaClass map;
	static private ClassNotFoundException storedException;

	static {
		try {
			serializable = Repository.lookupClass("java.io.Serializable");
			collection = Repository.lookupClass("java.util.Collection");
			map = Repository.lookupClass("java.util.Map");
		} catch (ClassNotFoundException e) {
			storedException = e;
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

		String refName = getComponentClass(refSig);
		if (refName.equals("java.lang.Object"))
			return 0.99;

		JavaClass refJavaClass = Repository.lookupClass(refName);
		return isDeepSerializable(refJavaClass);
	}

	public static String getComponentClass(String refSig) {
		while (refSig.charAt(0) == '[')
			refSig = refSig.substring(1);
		assert refSig.charAt(0) == 'L';
		String refName = refSig.substring(1, refSig.length() - 1).replace('/',
				'.');
		return refName;
	}

	public static double isDeepSerializable(JavaClass x)
			throws ClassNotFoundException {
		if (storedException != null)
			throw storedException;

		double result = deepInstanceOf(x, serializable);
		if (result >= 0.9)
			return result;
		result = Math.max(result, deepInstanceOf(x, collection));
		if (result >= 0.9)
			return result;
		result = Math.max(result, deepInstanceOf(x, map));
		return result;
	}

	/**
	 * Given two JavaClasses, try to estimate the probability that an reference
	 * of type x is also an instance of type y. Will return 0 only if it is
	 * impossiblem and 1 only if it is guaranteed.
	 * 
	 * @param x
	 *            Known type of object
	 * @param y
	 *            Type queried about
	 * @return 0 - 1 value indicating probablility
	 */
	public static double deepInstanceOf(JavaClass x, JavaClass y)
			throws ClassNotFoundException {

		if (x.equals(y))
			return 1.0;
		boolean xIsSubtypeOfY = Repository.instanceOf(x, y);
		if (xIsSubtypeOfY)
			return 1.0;
		boolean yIsSubtypeOfX = Repository.instanceOf(y, x);
		if (!yIsSubtypeOfX) {
			if (x.isFinal() || y.isFinal())
				return 0.0;
			if (!x.isInterface() && !y.isInterface())
				return 0.0;
		}

		Subtypes subtypes = AnalysisContext.currentAnalysisContext()
				.getSubtypes();

		Set<JavaClass> xSubtypes = subtypes.getTransitiveSubtypes(x);

		Set<JavaClass> ySubtypes = subtypes.getTransitiveSubtypes(y);

		Set<JavaClass> both = new HashSet<JavaClass>(xSubtypes);
		both.retainAll(ySubtypes);
		Set<JavaClass> xButNotY = new HashSet<JavaClass>(xSubtypes);
		xButNotY.removeAll(ySubtypes);

		if (yIsSubtypeOfX && both.isEmpty()) {
			System.out.println("Strange: y is subtype of x, but no classes in both");
			System.out.println("X : " + x.getClassName());
			for(JavaClass c : xSubtypes)
				System.out.println("  " + c.getClassName());
			System.out.println("Y : " + y.getClassName());
			for(JavaClass c : xSubtypes)
				System.out.println("  " + c.getClassName());
			
		}
		boolean concreteClassesInXButNotY = containsConcreteClasses(xButNotY);

		if (both.isEmpty()) {
			if (concreteClassesInXButNotY) {
				return 0.1;
			}
			return 0.3;
		}

		// exist classes that are both X and Y

		if (!concreteClassesInXButNotY) {
			// only abstract/interfaces that are X but not Y
			return 0.99;
		}

		// Concrete classes in X but not Y
		return 0.7;

	}
}
