package sfBugs;

import java.util.HashMap;
import java.util.Map;

public class Bug2877962 {

    HashMap<String, String> map = new HashMap<String, String>();

    public Map<String, String> getMap() {
        return new HashMap<String, String>();
    }

    public void doBadThings1() {
        Map<String, String> map = new HashMap<String, String>();
        map.get(1); // findbugs catches this one
    }

    public void doBadThings2() {
        new HashMap<String, String>().get(1); // not this one=
    }

    public void doBadThings3() {
        getMap().get(1); // or this one
    }

    public void doBadThings4() {
        map.get(1); // or this one
    }
}
