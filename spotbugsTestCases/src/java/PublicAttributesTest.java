// This class is more than a plain structure which stores data. It also has a
// public method which means that it has some kind of "behavior". Its data
// fields should not be visible from the outside.

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PublicAttributesTest {
    public int attr1 = 0;

    public int attr2;

    public static int sattr1 = 0;

    // A final field. It cannot be written in methods.
    public final int const1 = 10;

    public final Map<Integer, String> hm = new HashMap<Integer, String>();

    public final static Map<Integer, String> SHM = new HashMap<Integer, String>();

    // This field is not written, but only read.
    public final Set<String> hs = new HashSet<String>();

    public final String[] items = { "foo", "bar", "baz" };

    public static String[] sitems = { "foo", "bar", "baz" };

    public static final String[] SFITEMS = { "foo", "bar", "baz" };

    public PublicAttributesTest(int a1) {
        attr1 = a1;
        // attr2 = a1; // attr2 is not initialized, so if this line is not commented out, the error marker will be here
        // if static fields are not excluded in the detector,  the error marker for sattr1 is here,
        // even if there is an initialization, since the init of static fields is not in a constructorlike method
        sattr1 = a1;
    }

    public void doSomething() {
        attr1 = 1;
        attr2 = 2;
        sattr1 = 1;
        hm.put(1, "test");
        SHM.put(1, "test");
        boolean b = hs.contains("x");
        items[0] = "test";
        sitems[0] = "test";
        SFITEMS[0] = "test";
    }
}
