package deref;

class CorrectHandlingOfNoInformation {

	static interface A {
		int f(Object x);
	}

	static class B implements A {
		public int f(Object x) {
			return 0;
		}
	}

	static class C implements A {
		public int f(Object x) {
			return x.hashCode();
		}
	}

	public int g(A a, Object x) {
		return a.f(x);
	}

	public int h() {
		return g(new B(), null);
	}
}
