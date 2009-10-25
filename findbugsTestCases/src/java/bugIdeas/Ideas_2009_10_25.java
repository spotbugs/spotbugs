package bugIdeas;

import java.util.concurrent.CountDownLatch;

public class Ideas_2009_10_25 {
	CountDownLatch latch = new CountDownLatch(1);
	int x;
	
	public void set(int x) {
		latch.countDown();
		this.x = x;
	}
	
	public void increment() {
		synchronized(latch) {
			x++;
		}
	}
	
	public int get() {
		try {
	        latch.wait();
        } catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		return x;
	}

	public int get(int millis) {
		try {
	        latch.wait(millis);
        } catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		return x;
	}

}
