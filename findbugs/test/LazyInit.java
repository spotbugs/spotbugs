public class LazyInit {
	static Object foo;

	// This should be reported
	public static Object getFoo() {
		if (foo == null)
			foo = new Object();
		return foo;
	}

	// This should not be reported
	public synchronized static Object sgetFoo() {
		if (foo == null)
			foo = new Object();
		return foo;
	}
}
