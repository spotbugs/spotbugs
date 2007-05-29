package sfBugs;

public class Bug1726846 {

	private static final int[] INTS = { 1, 2, 3 };

	public static final int[] getInts() {
		return INTS;
	}

	private static int theStatic = 0;

	public void bug() {
		// Here is a write to a static field from an instance method
		theStatic = 17;
	}

}
