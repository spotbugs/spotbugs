public class NullDeref7 {

	private void alwaysThrow() {
		throw new RuntimeException();
	}

	public void foo() {
		Object o = null;
		if (o == null)
			alwaysThrow();
		System.out.println(o.hashCode());
	}
}
