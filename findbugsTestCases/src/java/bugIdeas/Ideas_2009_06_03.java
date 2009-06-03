package bugIdeas;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Ideas_2009_06_03<K,V> {
	
	
	ConcurrentMap<K,V> map= new ConcurrentHashMap<K,V>();
	public V atomicPut(K k, V v) {
		synchronized(map) {
			V v2 = map.get(k);
			if (v2 != null)
				return v2;
			map.put(k,v);
			return v;
		}
	}

}
