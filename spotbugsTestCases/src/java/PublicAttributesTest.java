// This class is more than a plain structure which stores data. It also has a
// public method which means that it has some kind of "behavior". Its data
// fields should not be visible from the outside.

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class PublicAttributesTest {
    @ExpectWarning("PA")
    public int attr1 = 0;

    // A final field. It cannot be written in methods.
    @NoWarning("PA")
    public final int Const1 = 10;

    @ExpectWarning("PA")
    public static final Map<Integer, String> hm = new HashMap<Integer, String>();
    // This field is not written, but only read.
    @NoWarning("PA")
    public static final Set<String> hs = new HashSet<String>();

    @ExpectWarning("PA")
    public static final String[] items = { "foo", "bar", "baz" };

    public PublicAttributesTest(int a1) {
        attr1 = a1;
    }

    public void doSomething() {
        attr1 = 1;
        hm.put(1, "test");
        boolean b = hs.contains("x");
        items[0] = "test";
    }
}
