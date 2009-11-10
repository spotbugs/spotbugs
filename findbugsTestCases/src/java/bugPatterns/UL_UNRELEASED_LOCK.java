package bugPatterns;

import java.util.concurrent.locks.Lock;

public class UL_UNRELEASED_LOCK {
	
	void bug(Lock any) {
		any.lock();
		// any code
	}
	void notBug(Lock any) {
		any.lock();
		try {
			// any code
		} finally {
			any.unlock();
		}
	}

}
