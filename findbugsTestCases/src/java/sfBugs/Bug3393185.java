package sfBugs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Bug3393185 {

    private String fileName_;

    private Map<String, Thread> tags_;

    private Set<String> events_;

    public Bug3393185(String fileName) {
        fileName_ = fileName;
        tags_ = new HashMap<String, Thread>();
        events_ = new HashSet<String>();
    }

}
