package edu.umd.cs.findbugs;

public class SignatureConverter {
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

// vim:ts=4
