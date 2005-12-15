public class InnerIS {
	private int x;

	class Inner {
		public int f() {
			synchronized (InnerIS.this) {
				return x;
			}
		}
	}

	public synchronized int get() {
		return x;
	}

	public synchronized void set(int v) {
		x = v;
	}

}

// vim:ts=4
