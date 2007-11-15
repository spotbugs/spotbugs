package sfBugs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bug1830576 {
    
    public Set<String> keySet() {
        return null;
    }

    public String get(String key) {
        return null;
    }

    public static void main(String[] args) {
        Map<String, String> realMap = new HashMap<String, String>();
        Bug1830576 fakeMap = new Bug1830576();
        for (String key : fakeMap.keySet()) {
            String value = fakeMap.get(key);
            System.out.println(value + realMap.get(key));
        }
    }
}
