public class SleepWithLock {
	boolean ready;

	void sleepWithLock() throws InterruptedException {

		synchronized (this) {
			while (!ready) {
				Thread.sleep(1000L);
			}
		}
	}
}
