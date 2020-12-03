// This class is just a structure to store data. It does not have any methods,
// except a constructor which initializes some of the fields. Therefore public
// attributes can be allowed here.

import edu.umd.cs.findbugs.annotations.NoWarning;

import java.util.Map;
import java.util.HashMap;

class PublicAttributesNegativeTest {
    @NoWarning("PA")
    public int attr1 = 0;

    @NoWarning("PA")
    public static final Map<Integer, String> hm = new HashMap<Integer, String>();

    @NoWarning("PA")
    public static final String[] items = { "foo", "bar", "baz" };

    PublicAttributesNegativeTest(int a1) {
        attr1 = a1;
    }
}
