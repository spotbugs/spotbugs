package sfBugsNew;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1422 {
    private final Map<TimeUnit, String> map1;
    private Map<TimeUnit, String> map2;
    
    public Bug1422(boolean b) {
        map1 = new EnumMap<>(TimeUnit.class);
        if(b) {
            map2 = new HashMap<>();
        } else {
            map2 = new EnumMap<>(TimeUnit.class);
        }
    }
    
    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void iterateEnumMap(EnumMap<TimeUnit, String> map) {
        for(TimeUnit u : map.keySet()) {
            System.out.println(u+": "+map.get(u));
        }
    }
    
    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void iterateMap(Map<TimeUnit, String> map) {
        for(TimeUnit u : map.keySet()) {
            System.out.println(u+": "+map.get(u));
        }
    }
    
    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void iterateEnumMapField() {
        for(TimeUnit u : map1.keySet()) {
            System.out.println(u+": "+map1.get(u));
        }
    }
    
    // We are not sure about map2 type: it can be HashMap as well
    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void iterateMapField() {
        for(TimeUnit u : map2.keySet()) {
            System.out.println(u+": "+map2.get(u));
        }
    }
}
