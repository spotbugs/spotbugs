package npe;

public class NullDeref7 {

	private void alwaysThrow() {
		throw new RuntimeException("Oops");
	}

	private void throwIfNull(Object o) {
		if (o == null)
			throw new RuntimeException("Got a null pointer");
	}

	public void foo(Object o) {
		if (o == null)
			alwaysThrow();
		System.out.println(o.hashCode());
	}

	public void bar(Object o) {
		if (o == null)
			throwIfNull(o);
		System.out.println(o.hashCode());
	}
}
