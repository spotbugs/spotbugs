import java.util.regex.*;

class BadRegEx {

	boolean f(String s) {
		return s.matches("][");
	}

	String g(String s) {
		return s.replaceAll("][", "xx");
	}

	String h(String s) {
		return s.replaceFirst("][", "xx");
	}

	void x(String s) throws Exception {
		Pattern.matches("][", s);
	}

	Pattern y(String s) throws Exception {
		return Pattern.compile("][", Pattern.CASE_INSENSITIVE);
	}

	Pattern z(String s) {
		return Pattern.compile("][");
	}

	Pattern literalOne(String s) throws Exception {
		return Pattern.compile("][", Pattern.LITERAL); // not a bug
	}

	Pattern literalTwo(String s) throws Exception {
		return Pattern.compile("][", Pattern.CASE_INSENSITIVE | Pattern.LITERAL); // not a bug
	}

	// this is OK; we shouldn't report a warning here
	String passwordMasking(String s) {
		return s.replaceAll(".", "x");
	}
}
