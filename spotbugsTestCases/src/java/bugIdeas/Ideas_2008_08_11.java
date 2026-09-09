package bugIdeas;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class Ideas_2008_08_11 {

    @Test
    public void fooBar() throws Exception {
        Assertions.assertFalse(Boolean.TRUE.equals(5));
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        Assertions.assertFalse(map.containsKey(1));
        Assertions.assertNull(map.get(1));
    }

    @Test
    public void fooBar2() throws Exception {
        Assertions.assertFalse(Boolean.TRUE.equals(5));
        HashSet<String> set = new HashSet<String>();
        set.add("a");
        Assertions.assertFalse(set.contains(1));
        Assertions.assertFalse(set.remove(1));
    }

    public void testFoo() throws Exception {
        Assertions.assertFalse(Boolean.TRUE.equals(5));
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        Assertions.assertFalse(map.containsKey(1));
        Assertions.assertNull(map.get(1));
        Assertions.assertNull(map.remove(1));
    }

}
