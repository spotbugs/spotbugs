class SpinWait {
	boolean flag;

	volatile boolean vflag;

	void waitForTrue() {
		while (flag)
			;
	}

	void waitForVolatileTrue() {
		while (vflag)
			;
	}

	SpinWait foo;

	SpinWait bar;

	void waitForNonNull() {
		while (foo == null)
			;
	}

	static void waitForNonNullIndirect(int x, SpinWait baz) {
		while (baz.foo == null)
			;
		while (baz.foo.bar == null)
			;
	}

	static boolean sflag;

	static void waitForStatic() {
		while (!sflag)
			;
	}
}
