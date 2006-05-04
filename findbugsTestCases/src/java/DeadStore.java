public class DeadStore {

	int x, y;

	public static void main(String[] args) {
		String live = "Hello, world";
		String dead = "Oops!";

		System.out.println(live);
		args = new String[0];
	}

	public int finalLocalDNR(int a) {
		final int SCAN = 0; // <<---- complains about this line
		final int STAR = 1;
		final int DONE = 2;
		// int state = SCAN;

		a += SCAN;
		a += STAR;
		a += DONE;

		return a;
	}

	public void duplicateDeadStores() {
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
		try {
			Object o = new Object();
		} catch (RuntimeException e) {
		}
	}

	public int storeNullDNR(int a) {
		Object foo = null;
		return a;
	}

	public int storeZeroDNR(int a) {
		int count = 0;

		return a;
	}

	public int killedByStoreDNR(int a) {
		int b = 3;

		if (a > 1) {
			b = 4;
			a += b;
		}

		return a;
	}

	public int notReportedin086(Object o) {
		if (o instanceof String) {
			String s = (String) o; // Not reported in 0.8.6 but reported in
			// 0.8.5 (Bug: 1105217)
		}
		return o.hashCode();
	}

	public int cachingFields(int a, int b, int c, int d, int e) {
		a = x;
		b = 5;
		c = x + 1;
		d = hashCode();
		return e;
	}
}
