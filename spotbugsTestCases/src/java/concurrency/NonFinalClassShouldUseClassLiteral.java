package concurrency;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class NonFinalClassShouldUseClassLiteral {
    private static int count;

    @ExpectWarning("WL")
    public NonFinalClassShouldUseClassLiteral() {
        synchronized (getClass()) {
            count++;
        }
    }
}
