import java.io.Serializable;

public class Blat implements Runnable, java.util.Iterator, Serializable {
	private final int yarg = 2;

	private final int yarm;

	private Object lock;

	public Blat(int y) {
		yarm = y;
	}

	private static class Y extends Thread {
		@Override
		public void run() {
		}
	}

	private static class NotThread {
		public void start() {
			System.out.println("Don't report this");
		}
	}

	public Blat() {
		yarm = 5;
		System.out.println(lock);
		new Y().start();
		new NotThread().start();
	}

	public void xxx() {
		Y y = new Y();
		y.run();
	}

	public int greeb() {
		return yarg;
	}

	public class Bleem {
		public void gnasp() {
			System.out.println("oog");
		}

		@Override
		protected void finalize() {
		}
	}

	public Bleem makeBleem() {
		return new Bleem();
	}

	@Override
	public void finalize() {
		System.out.println("This is dumb");
	}

	public void other(Blat b) {
		b.finalize();
		System.out.println(new Boolean(true));
		b.run();
	}

	public void run() {
	}

	public void badlock() {
		lock = new Object();

		greeb();
		synchronized (lock) {
			makeBleem();
		}

		System.out.println(new String());
	}

	private int yoom;

	public int getYoom() throws InterruptedException {
		Object x = lock;
		synchronized (x) {
			x.notify();
		}

		synchronized (x) {
			x.wait();
		}

		System.out.println(new String("hello"));
		return yoom;
	}

	public synchronized void setYoom(int y) {
		yoom = y;
	}

	public void gimme(java.io.InputStream in) {
		try {
			byte[] buf = new byte[256];
			in.read(buf);
			System.out.println(new String(buf));
		} catch (java.io.IOException e) {
		}
	}

	public void spin() {
		while (lock == null)
			;
	}

	public boolean hasNext() {
		return false;
	}

	public Object next() {
		return null;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private long serialVersionUID = 11091284L;

	private static Object[] foobar = new Object[1];

	public static Object[] getFoobar() {
		return foobar;
	}
}
