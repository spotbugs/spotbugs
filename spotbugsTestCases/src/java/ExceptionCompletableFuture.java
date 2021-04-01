import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExceptionCompletableFuture {

    public CompletableFuture<Void> returned() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        return future;
    }

    public void awaitedWithJoin() {
        returned().join();
    }

    public void awaitedWithGet() throws ExecutionException, InterruptedException {
        returned().get();
    }

    public void awaitedWithGetTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        returned().get(1L, TimeUnit.SECONDS);
    }

    public void chainedAndHandled() {
        CompletableFuture<Void> future = returned();
        future.thenAccept((v) -> {

        }).exceptionally((ex) -> {
            System.out.println("Found exception");
            ex.printStackTrace();
            return null;
        });
    }

    public void swallowed() {
        returned().thenRun(() -> { });
    }

}
