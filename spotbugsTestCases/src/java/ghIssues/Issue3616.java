package ghIssues;

import java.util.concurrent.locks.ReentrantLock;

public class Issue3616 {
	private final ReentrantLock evictionLock = new ReentrantLock();
	
	public void performCleanUp(boolean flag) {
		evictionLock.lock();
		try {
			maintenance(flag);
		} finally {
			evictionLock.unlock();
		}
	}

	public void maintenance(boolean flag) {
		if (flag) {
			throw new IllegalStateException();
		}
	}
}
