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
		if (o2 == null) { // medium priority
			System.out.println("This is silly");
		}
	}
}

// vim:ts=4
