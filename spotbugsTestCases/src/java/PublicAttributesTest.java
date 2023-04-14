// This class is more than a plain structure which stores data. It also has a
// public method which means that it has some kind of "behavior". Its data
// fields should not be visible from the outside.

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PublicAttributesTest {
    public int attr1 = 0;

    public static int sattr1 = 0;

    // A final field. It cannot be written in methods.
    public final int const1 = 10;

    public final Map<Integer, String> hm = new HashMap<Integer, String>();

    public final static Map<Integer, String> SHM = new HashMap<Integer, String>();

    // This field is not written, but only read.
    public final Set<String> hs = new HashSet<String>();

    public final String[] items = { "foo", "bar", "baz" };

    public static final String[] SITEMS = { "foo", "bar", "baz" };

    public PublicAttributesTest(int a1) {
        attr1 = a1;
    }

    public void doSomething() {
        attr1 = 1;
        sattr1 = 1;
        hm.put(1, "test");
        SHM.put(1, "test");
        boolean b = hs.contains("x");
        items[0] = "test";
        SITEMS[0] = "test";
    }
}
