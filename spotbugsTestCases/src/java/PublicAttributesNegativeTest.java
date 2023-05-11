// This class is just a structure to store data. It does not have any methods,
// except a constructor which initializes some of the fields. Therefore public
// attributes can be allowed here.

import java.util.HashMap;
import java.util.Map;

class PublicAttributesNegativeTest {
    public int attr1 = 0;

    public static final Map<Integer, String> HM = new HashMap<Integer, String>();

    public static final String[] ITEMS = { "foo", "bar", "baz" };

    PublicAttributesNegativeTest(int a1) {
        attr1 = a1;
    }
}
