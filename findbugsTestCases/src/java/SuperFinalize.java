class BaseClass {
	@Override
    protected void finalize() throws Throwable {
		System.out.println("A.finalize()");
	}
}

class SuperFinalize extends A {
	@Override
    protected void finalize() throws Throwable {
		System.out.println("B.finalize()");
		super.finalize();
	}
}
