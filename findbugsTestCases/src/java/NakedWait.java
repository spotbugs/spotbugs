
class NakedWait {
	boolean ready;

	public void makeReady() {
		ready = true;
		synchronized (this) {
			notify();
		}
	}

	public void waitForReady() {
		while (!ready) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
