public class LazyInit {
	static Object foo;

	static volatile Object bar;

	// This should be reported
	public static Object getFoo() {
		if (foo == null)
			foo = new Object();
		return foo;
	}

	// This should be reported
	public static Object getBar() {
		if (bar == null)
			bar = new Object();
		return bar;
	}

	// This should not be reported
	public synchronized static Object sgetFoo() {
		if (foo == null)
			foo = new Object();
		return foo;
	}
}
