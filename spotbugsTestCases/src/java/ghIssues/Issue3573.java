package ghIssues;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

public class Issue3573 {
	public boolean doSomethingCompletelyDifferent(Map<String, String> map) {
		boolean b = true;
		if(map.get("") != null) {
			b = map.get("").contains("42"); // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE warning here
		}
		return b;
	}
	
	public static class HashBiDictionnary<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;

		@Override
		@CheckForNull
		public V get(Object key) {
			return super.get(key);
		}
	}
}