public class TwoLockWait {


	public void myMethod(Object a, Object b) {
		try {
		synchronized (a) {
			synchronized (b) {
				a.wait();
			}
		}
		} catch (InterruptedException e) {
			System.out.println("Interrupted");
		}
	}

}
