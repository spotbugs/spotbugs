import java.util.concurrent.locks.*;

class JSR166 {
	Lock l = new ReentrantLock();
	int x;

	void l() {
		l.lock();
		}

	void foo() {
		l.lock();
		x++;
		if (x >= 0) 
			l.unlock();
		}

	void increment() {
		l.lock();
		x++;
		l.unlock();
		}

	void decrement() {
		l.lock();
		try {
		  x++;
		} finally {
		  l.unlock();
		  }
		}
	}



	
	
