package bugIdeas;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * A fellow Googler made an excellent point that ignoring to call Future.get() on the returned Future of java.util.concurrent.ExecutorService.submit(...) is as bad as an empty catch clause. Worse even, since it's not quite apparent that this is what you're doing.
 * @author Andy Staudacher
 *
 */
public class Ideas_2010_11_23<T> {
    
    @ExpectWarning("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public  void test(ExecutorService service, Callable<T> callable, Runnable runnable, T value) {
        service.submit(callable);
        service.submit(runnable);
        service.submit(runnable, value);
        
    }
    @ExpectWarning("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public  void test(ThreadPoolExecutor service, Callable<T> callable, Runnable runnable, T value) {
        service.submit(callable);
        service.submit(runnable);
        service.submit(runnable, value);
        
    }
    @ExpectWarning("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    
    public  void test(ScheduledThreadPoolExecutor service, Callable<T> callable, Runnable runnable, T value) {
        service.submit(callable);
        service.submit(runnable);
        service.submit(runnable, value);
        
    }
    @ExpectWarning("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    
    public  void test(AbstractExecutorService service, Callable<T> callable, Runnable runnable, T value) {
        service.submit(callable);
        service.submit(runnable);
        service.submit(runnable, value);
        
    }

}
