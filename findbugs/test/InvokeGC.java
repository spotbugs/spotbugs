public class InvokeGC {
	public static void main(String[] argv) {
		System.gc();
		System.currentTimeMillis();
	}

	public void finalize() {
		System.gc();
	}

	public void bad() {
		System.gc();
	}
}
