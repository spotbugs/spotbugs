package sfBugs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Bug2983950 implements Serializable {

    private static final long serialVersionUID = 0;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private final Map<String, String> map1 = new NonSerializableMap(); // = new
                                                                       // LinkedHashMap<String,
                                                                       // String>();

    private final Map<String, String> map2;

    private final Map<String, String> map3 = new LinkedHashMap<String, String>();

    public Bug2983950(Map<String, String> m) {
        map2 = m;
    }

    private final X x = new X();

    static class X {
        int y;
    }

    static class NonSerializableMap extends AbstractMap {

        @Override
        public Set entrySet() {
            return Collections.emptySet();
        }

    }

}
