public class RedundantNullCheck {
	public void foo(String s) {
		System.out.println(s.hashCode());

		if (s != null) { // bug
			System.out.println("Yeah");

			if (s != null) { // not as bad - low priority warning
				System.out.println("Oh my");
			}
		}

		Object o = new Object();
		if (o != null) { // low priority
			System.out.println(o.hashCode());
		}

		Object o2 = null;
		if (o2 == null) { // low priority
			System.out.println("This is silly");
		}
	}

   public int baz(String s) {
	if (s.hashCode() > 0) return s.hashCode();
	if (s == null) throw new NullPointerException("Foo");
    return -s.hashCode();
	}

	public int bar(String s) {
		try {
		if (s == null)
			return 17;
		else return 42;
		} finally {
		// Should not signal a RCN warning here
		if (s != null) System.out.println("foo");
		}
		}
		
		
}

// vim:ts=4
