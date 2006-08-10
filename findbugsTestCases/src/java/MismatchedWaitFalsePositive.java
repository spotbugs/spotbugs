public class MismatchedWaitFalsePositive {
	Object lock;

	MismatchedWaitFalsePositive(Object x) {
		lock = x;
	}

	public void waitOnLock() {
		synchronized (lock) {
			while (true) {
				try {
					lock.wait();
					return;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void notifyAllOnLock() {
		synchronized (lock) {
			lock.notify();
		}
	}

}
