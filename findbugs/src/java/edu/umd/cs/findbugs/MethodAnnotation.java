package edu.umd.cs.findbugs;

public class MethodAnnotation extends PackageMemberAnnotation {
	private String methodName;
	private String methodSig;
	private String fullMethod;

	public MethodAnnotation(String className, String methodName, String methodSig) {
		super(className);
		this.methodName = methodName;
		this.methodSig = methodSig;
		fullMethod = null;
	}

	public String getMethodName() { return methodName; }

	public String getMethodSignature() { return methodSig; }

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return getFullMethod();
		else
			throw new IllegalArgumentException("unknown key " + key);
	}

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

/*
	public static void main(String[] argv) {
		MethodAnnotation m = new MethodAnnotation("edu.umd.cs.daveho.ba.CFG", "fooIterator",
			"(I[[BLjava/util/Iterator;Ljava/lang/String;)Ledu/umd/cs/daveho/ba/BasicBlock;");
		System.out.println(m.toString());
	}
*/
}

// vim:ts=4
