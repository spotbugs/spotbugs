package edu.umd.cs.findbugs;

/**
 * Convert part or all of a Java type signature into something
 * closer to what types look like in the source code.
 * Both field and method signatures may be processed by this class.
 * For a field signature, just call parseNext() once.
 * For a method signature, parseNext() must be called multiple times,
 * and the parens around the arguments must be skipped manually
 * (by calling the skip() method).
 *
 * @author David Hovemeyer
 */
public class SignatureConverter {
	private String signature;

	/**
	 * Constructor.
	 * @param signature the field or method signature to convert
	 */
	public SignatureConverter(String signature) {
		this.signature = signature;
	}

	/**
	 * Get the first character of the remaining part of the signature.
	 */
	public char getFirst() {
		return signature.charAt(0);
	}

	/**
	 * Skip the first character of the remaining part of the signature.
	 */
	public void skip() {
		signature = signature.substring(1);
	}

	/**
	 * Parse a single type out of the signature, starting at the beginning
	 * of the remaining part of the signature.  For example, if the first
	 * character of the remaining part is "I", then this method will return
	 * "int", and the "I" will be consumed.  Arrays, reference types,
	 * and basic types are all handled.
	 * @return the parsed type string
	 */
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

// vim:ts=4
