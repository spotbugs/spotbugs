public class NullDeref {

	private int x;

	public static void main(String[] argv) {
		NullDeref n = new NullDeref();

		n = null;

		if (n == null) {
			System.out.println("Hey yo, it's null");
			System.out.println("Bad idea: " + n.x);
		} else {
			System.out.println("Safe to deref here: " + n.x);
		}

		// We don't detect that this can fail, because
		// currently we don't have a way of remembering that
		// n was originally null and that the IF statement didn't
		// modify it.
		n.x = 42;
	}

}
