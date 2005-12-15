class BaseClass {
	protected void finalize() throws Throwable {
		System.out.println("A.finalize()");
	}
}

class SuperFinalize extends A {
	protected void finalize() throws Throwable {
		System.out.println("B.finalize()");
		super.finalize();
	}
}
