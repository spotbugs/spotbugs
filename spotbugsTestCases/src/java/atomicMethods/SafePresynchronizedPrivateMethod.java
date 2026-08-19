package atomicMethods;

import java.util.concurrent.atomic.AtomicInteger;

public class SafePresynchronizedPrivateMethod {

    private final AtomicInteger currentBucket = new AtomicInteger();

    public void handleEvent() {
        currentBucket.incrementAndGet();
    }

    public void afterEvent() {
        synchronized (currentBucket) {
            queueFullBucket(currentBucket);
        }
    }

    private void queueFullBucket(AtomicInteger bucket) {
        bucket.incrementAndGet();
        if (bucket.get() == 10) {
            bucket.set(0);
        }
    }
}
