package bugIdeas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class Ideas_2008_11_14 extends TestCase {

    public void testOne() {
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("a", "a");
        assertFalse(m.containsKey(1));
        assertFalse(m.containsValue(1));
        assertFalse(m.entrySet().contains(1));
        assertFalse(m.keySet().contains(1));
        assertFalse(m.values().contains(1));
    }

    HashMap<String, String> m = new HashMap<String, String>();

    Set<Integer> is = new HashSet<Integer>();

    public void foo() {

        m.put("a", "a");
        Set<Map.Entry<Integer, Integer>> es = new HashSet<Map.Entry<Integer, Integer>>();
        boolean b1 = m.entrySet().contains(1); // bad
        boolean b2 = m.keySet().contains(1); // ok
        boolean b3 = m.values().contains(1); // ok
        boolean b4 = m.entrySet().equals(es); // ok
        boolean b5 = m.entrySet().equals(is); // bad
        m.entrySet().contains(1); // bad
        boolean b6 = m.keySet().equals(is); // ok
        boolean b7 = m.values().equals(is); // ok
        System.out.printf("%b %b %b %b %b %b %b\n", b1, b2, b3, b4, b5, b6, b7);
    }
}
