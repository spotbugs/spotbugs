public class MismatchedWait {

	public void foo(Object a, Object b) throws InterruptedException {
		synchronized (a) {
			b.wait();
		}
	}

	public void bar(Object a, Object b) throws InterruptedException {
		synchronized (a) {
			b.notify();
			b.notifyAll();
		}
	}

	private Object lock = new Object();

	public void doNotReport() throws InterruptedException {
		synchronized (lock) {
			while (lock.toString().equals("duh"))
				lock.wait();
		}
	}

	private static Object slock = new Object();

	public static void doNotReportStatic() throws InterruptedException {
		synchronized (slock) {
			while (slock.toString().equals("foobar"))
				slock.wait();
		}
	}

	public static void doNotReportClassRef() throws InterruptedException {
		synchronized (MismatchedWait.class) {
			while (slock.toString().equals("foobar"))
				MismatchedWait.class.wait();
		}
	}

	private int value = 0;

	public void doNotReportInnerClass() {
		new Runnable() {
			public void run() {
				synchronized (lock) {
					try {
						while (value == 0)
							lock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}.run();
	}
}

// vim:ts=4
