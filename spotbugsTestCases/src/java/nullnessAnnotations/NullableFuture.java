package nullnessAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <blockquote>Arguments used to pass a completion result (that is, for parameters of type T) for methods accepting them may be null, but passing a null value for any other parameter will result in a NullPointerException being thrown.</blockquote>
 * @see CompletableFuture
 * @see <a href="https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html">CompletableFuture JDK9</a>
 */
public class NullableFuture {
    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1001">GitHub issue #1001</a>
     */
    public void getNow() {
        // 1st argument of CompletableFuture#getNow(T) should be nullable
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.getNow(null);
    }

    public void completeOnTimeout() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeOnTimeout(null, 1L, TimeUnit.MINUTES);
    }

    public void obtrudeValue() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.obtrudeValue(null);
    }
}
