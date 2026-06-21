package ghIssues;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Issue3516 implements Runnable {

    private final CloseableReentrantLock lock = new CloseableReentrantLock();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private int consecutiveUnsuccessfulCalls;

    @Override
    public void run() {
        if (consecutiveUnsuccessfulCalls < 5) {
            doTask();
        }
    }

    public void start() {
        executor.schedule(this::doTask, 5, TimeUnit.MILLISECONDS);
    }

    void doTask() {
        try (var ignored = lock.open()) {
            try {
                Thread.sleep(1);
                consecutiveUnsuccessfulCalls = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                consecutiveUnsuccessfulCalls++;
            } finally {
                scheduleNextCall();
            }
        }
    }

    private void scheduleNextCall() {
        if (consecutiveUnsuccessfulCalls < 5) {
            executor.schedule(this::doTask, 5, TimeUnit.MILLISECONDS);
        }
    }

    static class CloseableReentrantLock extends ReentrantLock {

        Guard open() {
            this.lock();
            return new Guard();
        }

        class Guard implements AutoCloseable {

            @Override
            public void close() {
                CloseableReentrantLock.this.unlock();
            }
        }
    }
}
