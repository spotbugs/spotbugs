package sfBugs;

import java.util.HashMap;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3572704 {
    public static HashMap<String, String> map = new HashMap<String, String>();

    @ExpectWarning("DLS_DEAD_LOCAL_STORE")
    public static String test(String name) {
        int pos = 0;
        while (pos >= 0 && !name.isEmpty())
            ;
        {
            if (map.containsKey(name)) {
                return name;
            }
            pos = name.indexOf('.');
            name = name.substring(pos + 1);
            // ^^^ "Dead store to name" here
        }
        return null;
    }
    
    @NoWarning("DLS_DEAD_LOCAL_STORE")
    public static String test2(String name) {
        int pos = 0;
        while (pos >= 0 && !name.isEmpty())
        {
            if (map.containsKey(name)) {
                return name;
            }
            pos = name.indexOf('.');
            name = name.substring(pos + 1);
            // ^^^ "Dead store to name" here
        }
        return null;
    }
}
