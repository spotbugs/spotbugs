public class NullDeref {

	private int x;

	public static void main(String[] argv) {
		NullDeref n = new NullDeref();

		n = null;

		if (n == null) {
			System.out.println("Hey yo, it's null");
		} else {
			System.out.println("Safe to deref here: " + n.x);
		}

		// Oops, might be null!
		n.x = 42;
	}

}
