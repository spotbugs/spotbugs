package edu.umd.cs.findbugs;

public class MethodAnnotation implements BugAnnotation {
	private String className;
	private String methodName;
	private String methodSig;

	private String niceMethodName;

	public MethodAnnotation(String className, String methodName, String methodSig) {
		this.className = className;
		this.methodName = methodName;
		this.methodSig = methodSig;
		niceMethodName = null;
	}

	public String getClassName() { return className; }

	public String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			return "";
		else
			return className.substring(0, lastDot);
	}

	public String getMethodName() { return methodName; }

	public String getMethodSignature() { return methodSig; }

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	private static class SignatureConverter {
		private String signature;

		public SignatureConverter(String signature) {
			this.signature = signature;
		}

		public char getFirst() {
			return signature.charAt(0);
		}

		public void skip() {
			signature = signature.substring(1);
		}

		public String parseNext() {
			StringBuffer result = new StringBuffer();

			if (signature.startsWith("[")) {
				int dimensions = 0;
				do {
					++dimensions;
					signature = signature.substring(1);
				} while (signature.charAt(0) == '[');
				result.append(parseNext());

				while (dimensions-- > 0) {
					result.append("[]");
				}
			} else if (signature.startsWith("L")) {
				int semi = signature.indexOf(';');
				if (semi < 0)
					throw new IllegalStateException("missing semicolon in signature " + signature);
				result.append(signature.substring(1, semi).replace('/', '.'));
				signature = signature.substring(semi + 1);
			} else {
				switch (signature.charAt(0)) {
				case 'B': result.append("byte"); break;
				case 'C': result.append("char"); break;
				case 'D': result.append("double"); break;
				case 'F': result.append("float"); break;
				case 'I': result.append("int"); break;
				case 'J': result.append("long"); break;
				case 'S': result.append("short"); break;
				case 'Z': result.append("boolean"); break;
				default: throw new IllegalStateException("bad signature " + signature);
				}
				skip();
			}

			return result.toString();
		}
	}

	public String toString() {
		if (niceMethodName == null) {
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

			String returnType = shorten(pkgName, converter.parseNext());

			StringBuffer result = new StringBuffer();
			result.append(returnType);
			result.append(' ');
			result.append(className);
			result.append('.');
			result.append(methodName);
			result.append('(');
			result.append(args);
			result.append(')');

			niceMethodName = result.toString();
		}

		return niceMethodName;
	}

	private String shorten(String pkgName, String typeName) {
		if (typeName.startsWith(pkgName + "."))
			typeName = typeName.substring(pkgName.length() + 1);
		else if (typeName.startsWith("java.lang."))
			typeName = typeName.substring("java.lang.".length());
		return typeName;
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
