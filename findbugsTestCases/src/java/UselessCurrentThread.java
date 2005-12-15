
public class UselessCurrentThread implements Runnable {
	private Thread unknownThread;

	public UselessCurrentThread(Thread t) {
		unknownThread = t;
	}

	public void run() {
		try {
			// The first should be ok, the next two are silly, the third is
			// wrong
			while (!Thread.interrupted()) {
				System.out.println("huh?");
				Thread.sleep(10000);
			}

			while (!Thread.currentThread().interrupted()) {
				System.out.println("huh?");
				Thread.sleep(10000);
			}

			Thread t = Thread.currentThread();
			while (!t.interrupted()) {
				System.out.println("huh?");
				Thread.sleep(10000);
			}

			while (!unknownThread.interrupted()) {
				System.out.println("huh?");
				Thread.sleep(10000);
			}
		} catch (InterruptedException ie) {
			System.out.println("Oh, ok");
		}
	}
}