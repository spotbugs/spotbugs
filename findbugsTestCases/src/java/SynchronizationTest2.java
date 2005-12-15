class SynchronizationTest2 {
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

	public int get2X() {
		return x + x;
	}
}
