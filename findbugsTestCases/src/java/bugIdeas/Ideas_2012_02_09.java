package bugIdeas;

import java.util.TreeMap;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Ideas_2012_02_09 {
    
    @DesireWarning("GC")
    public static boolean foo(TreeMap<String, String> map, String key) {
        return map.keySet().contains(map);
    }

}
