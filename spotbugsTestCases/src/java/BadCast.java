import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class BadCast {

    Object myField;

    int fieldCheck() {
        if (myField instanceof String)
            return ((String) myField).length();
        return myField.hashCode();
    }

    public static <C extends Collection<?>> C smallest(Iterable<C> collections) {
        return null;
    }

    public static int sizeOfSmallest(Iterable<? extends Set<?>> sets) {
        // TODO: False positive BC here
        Set<?> s = smallest(sets);
        return s.size();
    }

    List a;

    @ExpectWarning("BC")
    public Vector swap(List b) {
        Vector v = (Vector) a;
        a = (Vector) b;
        return v;
    }

    Object foo() {
        return new Hashtable();
    }

    Map bar() {
        return new Hashtable();
    }

    Vector[] faz() {
        return new Vector[10];
    }

    Hashtable baz() {
        return new Hashtable();
    }

    @ExpectWarning("BC")
    int d() {
        Map m = bar();
        Set s = (Set) m.values();
        return s.size();
    }

    int f() {
        return ((Hashtable) foo()).size();
    }

    @ExpectWarning(value="BC_IMPOSSIBLE_CAST", num = 1)
    int f2() {
        Object o = faz();
        return ((Hashtable[]) o).length;
    }

    int h() {
        return ((Hashtable) bar()).size();
    }

    int h2() {
        Map m = bar();
        if (m instanceof Hashtable)
            return ((Hashtable) m).size();
        return 17;
    }

    int g() {
        return ((Hashtable[]) foo()).length;
    }

    @ExpectWarning("BC")
    int hx() {
        Object o = baz(); // returns Hashtable
        try {
            if (o instanceof Collection) {
                System.out.println("Yeah..." + ((Set) o).size());
            }
            if (o instanceof Stack)
                System.out.println("Strange...");
            else if (o instanceof Map)
                return ((Map) o).size();
            return ((Vector) o).size();
        } finally {
            if (o instanceof Map)
                System.out.println("Cool");
        }

    }

    static Object f(boolean b, Integer i, String s) {
        return b ? (Integer) i : (String) s;
    }

    @ExpectWarning(value="BC_IMPOSSIBLE_CAST", num = 1)
    public static int first(Object o) {
        if (o instanceof Byte[]) {
            byte b[] = (byte[]) o;
            return b[0];
        }
        return 0;
    }

    public static Serializable getSerializable() {
        return new String[0];
    }

    public static String[] doNotReport() {
        return (String[]) getSerializable();
    }

    public static int[] doNotReport2(Object x) {
        if (x instanceof int[])
            return (int[]) x;
        return null;
    }

    public static int[] doNotReport3(Object x, boolean b) {
        if (b)
            return (int[]) x;
        return null;
    }

    public static Serializable[] doNotReport4(List<Serializable[]> x) {
        return x.get(0);
    }

    @ExpectWarning("BC")
    public static int[] report(Object x) {
        if (x instanceof Object[])
            return (int[]) x;
        return null;

    }

    public static String[] modelMultiANewArrayCorrectlyDoNotReport(Object[] x) {
        Object[][] tmp = new Object[1][];
        tmp[0] = x;
        if (tmp[0] instanceof String[])
            return (String[]) tmp[0];
        return null;
    }

    public static String doNotReport(Object o) {
        if (o == null || o instanceof String)
            return ((String) o);
        else
            throw new IllegalArgumentException();
    }
}
