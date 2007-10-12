public class LazyInit {
	static Object foo;

	static volatile Object bar;

	
	static String [] x,y;

	public static String[] getX() {
		if (x != null) {
			x = new String[5];
			for(int i = 0; i < x.length; i++)
				x[i] = Integer.toString(i);
		}
		return x;
	}
	public static  String[] getY() {
		if (y != null) {
			y = new String[] {"a", "b", "c", "d", "e"};
		}
		return y;
	}
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
