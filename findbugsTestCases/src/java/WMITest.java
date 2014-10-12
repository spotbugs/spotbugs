import java.util.Iterator;
import java.util.Map;
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
        Iterator it = staticMap.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
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
