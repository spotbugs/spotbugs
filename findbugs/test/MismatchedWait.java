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
}
