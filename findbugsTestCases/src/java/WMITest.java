import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class WMITest {
    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void test(Map m) {
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            String value = (String) m.get(name);
            System.out.println(name + " = " + value);
        }
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void test2(int a, int b, int c, int d, Map m) {
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            String value = (String) m.get(name);
            System.out.println(name + " = " + value);
        }
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void test3(Map m) {
        Set s = m.keySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            String value = (String) m.get(name);
            System.out.println(name + " = " + value);
        }
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void test4(Map m) {
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            String value = (String) m.get(name);
            System.out.println(name.toString() + " = " + value);
        }
    }

    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void testIteratorLoadBug(Map<String, String> m1, List<String> list) {
        Set<String> keys = m1.keySet();
        //int a = 0;
        for(String str : list) {
            System.out.println(m1.get(str));
        }
    }

    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void testRegisterReuseBug(Map<String, String> m) {
        for(String key : m.keySet()) {
            System.out.println(key);
        }
        String k = "special";
        System.out.println(m.get(k));
    }

    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void testRegisterReuseBug2(Map<Integer, String> m) {
        int maxKey = 0;
        for(Integer key : m.keySet()) {
            if(key > maxKey)
                maxKey = key;
        }
        for(Integer i=0; i<maxKey; i++) {
            String val = m.get(i);
            System.out.println(i+": "+(val == null ? "none" : val));
        }
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void testTwice(Map<String, String> m1, Map<String, String> m2) {
        for(String m1key : m1.keySet()) {
            System.out.println(m1key);
        }
        for(String m2key : m2.keySet()) {
            System.out.println(m2key+"="+m2.get(m2key));
        }
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void testSingleElement(Map<String, String> m) {
        String key = m.keySet().iterator().next();
        String value = m.get(key);
        System.out.println(key+"="+value);
    }

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void testBoxing(Map<Integer, String> m) {
        for(int key : m.keySet()) {
            System.out.println(key+": "+m.get(key));
        }
    }

    private Map fieldMap = new HashMap();

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void testField() {
        Iterator it = fieldMap.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            String value = (String) fieldMap.get(name);
            System.out.println(name.toString() + " = " + value);
        }
    }

    private static Map staticMap = new HashMap();

    @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
    public void testStatic() {
        for(Object name : staticMap.keySet()) {
            String value = (String) staticMap.get(name);
            System.out.println(name.toString() + " = " + value);
        }
    }

    private Map fieldMap2 = new HashMap();
    @NoWarning("WMI_WRONG_MAP_ITERATOR")
    public void testWrongMap() {
        Iterator it = fieldMap.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            String value = (String) fieldMap2.get(name);
            System.out.println(name.toString() + " = " + value);
        }
    }

    public static class DebugHashMap extends HashMap {
        @ExpectWarning("WMI_WRONG_MAP_ITERATOR")
        public void dump() {
            Iterator it = keySet().iterator();
            while (it.hasNext()) {
                Object name = it.next();
                String value = (String) get(name);
                System.out.println(name.toString() + " = " + value);
            }
        }
    }
}
