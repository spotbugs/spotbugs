package edu.umd.cs.findbugs;

import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * A BugAnnotation specifying a particular method in a particular class.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class MethodAnnotation extends PackageMemberAnnotation {
	private String methodName;
	private String methodSig;
	private String fullMethod;

	/**
	 * Constructor.
	 * @param className the name of the class containing the method
	 * @param methodName the name of the method
	 * @param methodSig the Java type signature of the method
	 */
	public MethodAnnotation(String className, String methodName, String methodSig) {
		super(className);
		this.methodName = methodName;
		this.methodSig = methodSig;
		fullMethod = null;
	}

	/**
	 * Constructor.
	 * The method annotation is initialized using the method the given
	 * visitor is currently visiting.
	 * @param visitor the BetterVisitor
	 */
	public MethodAnnotation(BetterVisitor visitor) {
		super(visitor.getBetterClassName());
		this.methodName = visitor.getMethodName();
		this.methodSig = visitor.getMethodSig();
	}

	/** Get the method name. */
	public String getMethodName() { return methodName; }

	/** Get the method type signature. */
	public String getMethodSignature() { return methodSig; }

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return getFullMethod();
		else if (key.equals("shortMethod"))
			return className + "." + methodName + "()";
		else
			throw new IllegalArgumentException("unknown key " + key);
	}

	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 */
	public String getFullMethod() {
		if (fullMethod == null) {
			// Convert to "nice" representation
			SignatureConverter converter = new SignatureConverter(methodSig);
			String pkgName = getPackageName();

			StringBuffer args = new StringBuffer();

			if (converter.getFirst() != '(')
				throw new IllegalStateException("bad method signature " + methodSig);
			converter.skip();

			while (converter.getFirst() != ')') {
				if (args.length() > 0)
					args.append(',');
				args.append(shorten(pkgName, converter.parseNext()));
			}
			converter.skip();

			// NOTE: we omit the return type.
			// It is not needed to disambiguate the method,
			// and would just clutter the output.

			// Actually, GJ implements covariant return types at the source level,
			// so perhaps it really is necessary.

			StringBuffer result = new StringBuffer();
			result.append(className);
			result.append('.');
			result.append(methodName);
			result.append('(');
			result.append(args);
			result.append(')');

			fullMethod = result.toString();
		}

		return fullMethod;
	}

	public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof MethodAnnotation))
			return false;
		MethodAnnotation other = (MethodAnnotation) o;
		return className.equals(other.className)
			&& methodName.equals(other.methodName)
			&& methodSig.equals(other.methodSig);
	}

/*
	public static void main(String[] argv) {
		MethodAnnotation m = new MethodAnnotation("edu.umd.cs.daveho.ba.CFG", "fooIterator",
			"(I[[BLjava/util/Iterator;Ljava/lang/String;)Ledu/umd/cs/daveho/ba/BasicBlock;");
		System.out.println(m.toString());
	}
*/
}

// vim:ts=4
