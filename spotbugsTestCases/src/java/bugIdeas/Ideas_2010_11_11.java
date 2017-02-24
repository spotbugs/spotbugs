package bugIdeas;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_11_11<K,V> {
    final ConcurrentMap<K, V> map = new ConcurrentHashMap<K,V>();

    @NoWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public void remove(WeakReference<K> kRef, WeakReference<V> vRef) {
        K k = kRef.get();
        if (k != null)
            map.remove(k,vRef.get());
    }


}
