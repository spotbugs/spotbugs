import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class ConcurrentModificationBug extends TestCase {

    public void removeFoo(Map<String, Object> map) {

        // FIXME: We should generate a warning here
        for (String s : map.keySet())
            if (s.startsWith("foo"))
                map.remove(s);

    }

    public void testRemoveFoo() {
        HashMap<String, Object> s = new HashMap<String, Object>();
        s.put("foo1", "foo1");
        s.put("foo2", "foo1");
        s.put("bar", "foo1");
        removeFoo(s);

    }

}
