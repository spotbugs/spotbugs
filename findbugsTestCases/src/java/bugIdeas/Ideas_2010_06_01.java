package bugIdeas;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Ideas_2010_06_01<K,V> extends AbstractMap {

    static class NotWeird<K,V> extends AbstractMap<K,V> {

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

    }
    static abstract class NotWeird2<K,V> implements Map<K,V> {



    }
	@Override
    public Set entrySet() {
        return Collections.emptySet();
    }

    static Object test1(Ideas_2010_06_01<String, String> m) {
        return m.get((Integer) 1);
	}
    static Object test1a(Map<String, String> m) {
        return m.get((Integer) 1);
    }
	static Ideas_2010_06_01<String, String> builder() {
        return new Ideas_2010_06_01<String, String>();
    }
    static Map<String, String> builderA() {
		return new HashMap<String, String>();
    }
    static Object test2() {
        return builder().get((Integer) 1);
	}
    static Object test2a() {
        return builderA().get((Integer) 1);
    }
	

}
