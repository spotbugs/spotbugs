class SynchronizationTest5 {
	int x;

	public synchronized void add1() {
		x += 1;
	}

	public synchronized void add2() {
		x += 2;
	}

	public synchronized void add3() {
		x += 3;
	}

	public synchronized void add4() {
		x += 4;
	}

	public synchronized void add5() {
		x += 5;
	}

	public synchronized void add6() {
		x += 6;
	}

	int add(int y) {
		x += y;
		return x;
	}

	static class Foo {
		public int add(SynchronizationTest5 s, int y) {
			synchronized (s) {
				return s.add(y);
			}
		}
	}
}
