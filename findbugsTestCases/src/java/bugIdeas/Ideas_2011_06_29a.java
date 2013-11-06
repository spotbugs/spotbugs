package bugIdeas;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_06_29a {

  
    static boolean b() {
        return true;
    }

    @NoWarning("NP")
    public static void main(String args[]) {

        Exception e = new RuntimeException();
        if (b())
            e = new ExecutionException(null);
        if (b())
            e = new ExecutionException(null, null);

        if (b())
            e = new RejectedExecutionException((String) null);
        if (b())
            e = new RejectedExecutionException((Throwable) null);
        if (b())
            e = new RejectedExecutionException("test", null);
        if (b())
            e = new RejectedExecutionException(null, null);

        if (b())
            e = new BrokenBarrierException(null);
        if (b())
            e = new CancellationException(null);
        if (b())
            e = new TimeoutException(null);
        e.printStackTrace();

    }

}
