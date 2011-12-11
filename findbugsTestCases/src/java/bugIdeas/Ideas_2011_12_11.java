package bugIdeas;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Ideas_2011_12_11 {
    
    final CountDownLatch latch = new CountDownLatch(1);
    
    public void waitForIt() throws TimeoutException, InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
    }
    public void countDown() {
        latch.countDown();
    }

}
