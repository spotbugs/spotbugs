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
}
