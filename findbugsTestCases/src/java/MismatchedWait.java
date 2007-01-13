import java.util.HashMap;
import java.util.Map;

public class MismatchedWait {

    private static Map m = new HashMap();
    
    public void falsePositive(Object o) {
        synchronized(m) {
            m.put(o, o);
            m.notifyAll();
        }
    }
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

	private  Object lock = new Object();

	public void doNotReport() throws InterruptedException {
		synchronized (lock) {
			while (lock.toString().equals("duh"))
				lock.wait();
		}
	}

	private static Object slock = new Object();

	public static void doNotReportStatic() throws InterruptedException {
		synchronized (slock) {
			while (slock.toString().equals("foobar"))
				slock.wait();
		}
	}

	public static void doNotReportClassRef() throws InterruptedException {
		synchronized (MismatchedWait.class) {
			while (slock.toString().equals("foobar"))
				MismatchedWait.class.wait();
		}
	}
	
	public static Object getLock() {
		return slock;
	}
	static boolean ready = false;

	public static void doNotReportMethodCallWait() throws InterruptedException {
		synchronized (getLock()) {
			while (!ready)
				getLock().wait();
		}
	}
	public static void doNotReportMethodCallNotifyAll() throws InterruptedException {
		synchronized (getLock()) {
			ready = true;
			getLock().notifyAll();
		}
	}
	private int value = 0;

	public void doNotReportInnerClass() {
		new Runnable() {
			public void run() {
				synchronized (lock) {
					try {
						while (value == 0)
							lock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}.run();
	}
}

// vim:ts=4
