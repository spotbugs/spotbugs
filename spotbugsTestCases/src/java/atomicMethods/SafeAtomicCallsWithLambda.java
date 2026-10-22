package atomicMethods;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SafeAtomicCallsWithLambda {

    private final AtomicInteger pendingTasksCount;
    private final int maxPendingTasks = 5;

    public SafeAtomicCallsWithLambda() {
        pendingTasksCount = new AtomicInteger();
    }

    public CompletableFuture<Integer> addBlockRequest(int blockId) {
        tryIncrementTaskCount();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pendingTasksCount.decrementAndGet();
            return blockId;
        });
    }

    public CompletableFuture<Integer> addBlockRequest2(int blockId) {
        tryIncrementTaskCount();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pendingTasksCount.decrementAndGet();
            return blockId;
        });
    }

    private boolean tryIncrementTaskCount() {
        int currentCount = pendingTasksCount.updateAndGet(oldCount -> {
            if (oldCount == maxPendingTasks) {
                return oldCount;
            } else {
                return oldCount + 1;
            }
        });
        return currentCount < 10;
    }
}
