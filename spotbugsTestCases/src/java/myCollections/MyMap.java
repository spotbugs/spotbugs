package myCollections;

import java.util.HashMap;

public class MyMap<K, V> extends HashMap<K, V> {

    private static final long serialVersionUID = 1L;

    public void foo(Object x) {
    }

    public static void main(String args[]) {
        MyMap<Integer, Integer> m = new MyMap<Integer, Integer>();
        m.put(null, null);
        m.foo(null);

    }
}
