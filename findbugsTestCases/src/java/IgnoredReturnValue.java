import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	// This method shows one way to check to see if a string encoding name is legal.
	// Under earlier versions of the JVM, this may have been the best way to do this.
	// So we shouldn't signal a RV warning here
	public static boolean falsePositive(String enc) {
		try {
			new String(new byte[0], enc);
			return true;
		} catch (UnsupportedEncodingException e) {
			return false;
		}

	}

}
