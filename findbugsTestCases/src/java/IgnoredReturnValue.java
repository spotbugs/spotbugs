import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class IgnoredReturnValue {
	public static void main(String args[]) throws Exception {
		String str = " ttesting ";
		str.trim();
		str.equals("testing");
		Semaphore s = new Semaphore(17, true);
		s.tryAcquire();
		s.tryAcquire(12, TimeUnit.MILLISECONDS);
		BlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
		q.offer(new Object());
		q.offer(new Object(), 12, TimeUnit.MILLISECONDS);
		q.poll(12, TimeUnit.MILLISECONDS);
		q.poll();
		Lock l = new ReentrantLock();
		Condition c = l.newCondition();
		l.lock();
		try {
			c.awaitNanos(12);
			c.awaitUntil(new Date());
			c.await(12, TimeUnit.NANOSECONDS);
		} finally {
			l.unlock();
		}

		q.poll();
	}

}
