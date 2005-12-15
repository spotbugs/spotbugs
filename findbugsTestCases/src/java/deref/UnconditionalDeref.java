package deref;

public class UnconditionalDeref {

	private void f(Object obj) {
		System.out.println(obj.hashCode());
	}

	static void g(Object obj) {
		System.out.println(obj.hashCode());
	}

	void h(Object obj) {
		System.out.println(obj.hashCode());
	}

	void report() {
		f(null);
	}

	private void report2() {
		g(null);
	}

	private void report3() {
		// This should only be a medium priority warning,
		// because h may be overridden.
		h(null);
	}

	Object returnsNull() {
		return null;
	}

	// @Override
	public int hashCode() {
		return 0;
	}

	// We report equals() methods which unconditionally dereference the
	// parameter
	public boolean equals(Object o) {
		return o.hashCode() == this.hashCode() && o == this;
	}

	// We don't get this one currently
	void report4() {
		Object o = returnsNull();
		System.out.println(o.hashCode());
	}
}
