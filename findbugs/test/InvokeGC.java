public class InvokeGC {
	public static void main(String[] argv) {
		System.gc();
		System.currentTimeMillis();
	}

	protected void finalize() {
		System.gc();
	}

	public void bad() {
		System.gc();
	}

	public void ok() {
		try {
			System.out.println("ok()");
		} catch (OutOfMemoryError e) {
			System.gc();
		}
	}

	public void tricky() {
		try {
			System.out.println("tricky()");
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}

		System.gc();
	}
}
