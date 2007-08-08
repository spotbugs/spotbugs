package gcUnrelatedTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class ParameterizedAgainstPlain {

	Map<ScheduledFuture, List<Runnable>> afterHooks = new HashMap<ScheduledFuture, List<Runnable>>();

	protected void afterExecute(Runnable r, Throwable t) {

		List<Runnable> hooks = afterHooks.get(r); // should generate a warning here
		if (hooks != null) {

			for (Runnable hook : hooks) {
				hook.run();
			}
		}
	}
}
