package npe;

import java.util.HashMap;
import java.util.Map;

public class WithAssertions {
    Map<String, String> map = new HashMap<String, String>();

    int doNotReport(String s) {
        String result = map.get(s);
        assert result != null : "Result shouldn't be null";
        // don't report this
        return result.hashCode();
    }

}
