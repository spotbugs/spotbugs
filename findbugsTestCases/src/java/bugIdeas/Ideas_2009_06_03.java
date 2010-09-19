package bugIdeas;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Ideas_2009_06_03<K, V> {

    public static void main(String args[]) {
        ReferenceQueue<String> queue = new ReferenceQueue<String>();
        SoftReference<String> r = new SoftReference<String>(args[0], queue);
        r.get().hashCode();
        queue.poll().hashCode();
    }

    ConcurrentMap<K, V> map = new ConcurrentHashMap<K, V>();

    public V atomicPut(K k, V v) {
        synchronized (map) {
            V v2 = map.get(k);
            if (v2 != null)
                return v2;
            map.put(k, v);
            return v;
        }
    }

}
