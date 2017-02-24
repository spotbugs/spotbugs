package ghIssues;

import java.io.Serializable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Issue0076 {

    private static class Handler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
        }
    }
    
    @NoWarning("NP_NONNULL_PARAM_VIOLATION")
    public void testNominal() {
        new ForkJoinPool(2, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
    public void testWarning() {
        new ForkJoinPool(2, null, new Handler(), true);
    }
}
