package nonnull;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CheckJDKAnnotations {
	
	Future falsePositive(ExecutorService e, Runnable r) {
		return e.submit(r, null);
	}
	Future falsePositive(AbstractExecutorService e, Runnable r) {
		return e.submit(r, null);
	}

}
