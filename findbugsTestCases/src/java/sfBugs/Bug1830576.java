package sfBugs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bug1830576 {
    
    public Set<String> keySet() {
        return null;
    }

    public String get(String key) {
        return null;
    }

    public static void main(String[] args) {
        method0();
        method1();
        method2();
    }
    
    public static void method0() {
        // No warning
        Bug1830576 fakeMap = new Bug1830576();
        Map<String, String> realMap = new HashMap<String, String>();
        for (String key : fakeMap.keySet()) {
            String value = fakeMap.get(key);
            System.out.println(value + realMap.get(key));
        }
    }
    
    public static void method1() {
        Map<String, String> realMap = new HashMap<String, String>();
        for (String key : realMap.keySet()) {
            String value = realMap.get(key);
            System.out.println(value + realMap.get(key));
        }
    }
    
    public static void method2() {
        Map<String, String> realMap2 = new Bug1830576_helper<String, String>();
        for (String key : realMap2.keySet()) {
            String value = realMap2.get(key);
            System.out.println(value + realMap2.get(key));
        }
    }
    
    private static class Bug1830576_helper<K,V> implements Map<K,V> {

        public void clear() {
            // TODO Auto-generated method stub
            
        }

        public boolean containsKey(Object key) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean containsValue(Object value) {
            // TODO Auto-generated method stub
            return false;
        }

        public Set<java.util.Map.Entry<K, V>> entrySet() {
            // TODO Auto-generated method stub
            return null;
        }

        public V get(Object key) {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        public Set<K> keySet() {
            // TODO Auto-generated method stub
            return null;
        }

        public V put(K key, V value) {
            // TODO Auto-generated method stub
            return null;
        }

        public void putAll(Map<? extends K, ? extends V> t) {
            // TODO Auto-generated method stub
            
        }

        public V remove(Object key) {
            // TODO Auto-generated method stub
            return null;
        }

        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }

        public Collection<V> values() {
            // TODO Auto-generated method stub
            return null;
        }

        
    }
}
