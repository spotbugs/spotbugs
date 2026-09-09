package ghIssues;

import java.util.Map;

public class Issue3310 {
	private volatile int x;

	public <K, V> Object test(Map<K, V> map, K key) {
		Result result = new Result();
		map.compute(key, (k, oldValue) -> {
			V newValue = oldValue;
			result.written = true;
			return newValue;
		});

		if (result.written) {
			return result;
		} else {
			return null;
		}
	}

	public static class Result {
		boolean written;
	}
}
