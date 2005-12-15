public class T {

	Object lock = new Object();

	Object value;

	public synchronized void provideIt(Object v) {
		synchronized (lock) {
			value = v;
			lock.notifyAll();
		}
	}
}
