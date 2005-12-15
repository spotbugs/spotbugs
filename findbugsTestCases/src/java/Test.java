public class Test {
	public void foo() throws E {
		synchronized (this) {
			fooImpl();
		}
	}

	public synchronized void syncFoo() throws E {
		fooImpl();
	}

	public void fooImpl() throws E {
		throw new E();
	}

	public static void bar() throws E {
		synchronized (Test.class) {
			barImpl();
		}
	}

	public synchronized static void syncBar() throws E {
		barImpl();
	}

	public static void barImpl() throws E {
		throw new E();
	}

	public static void main(String[] argv) {
		Test t = new Test();

		try {
			t.foo();
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			t.syncFoo();
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			bar();
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			syncBar();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
