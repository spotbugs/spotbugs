public class EqualsComparison {
	void badEqualsComparision() {
		String s = "Hi there";
		Boolean b = Boolean.TRUE;

		System.out.println("equals() returned " + s.equals(b));
	}

	boolean literalStringEqualsDoNotReport(String s) {
		return "Uh huh".equals(s);
	}
}

// vim:ts=3
