package sfBugsNew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Bug1387 {

    static class PoJo {
        final String s;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((s == null) ? 0 : s.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PoJo other = (PoJo) obj;
            if (s == null) {
                if (other.s != null)
                    return false;
            } else if (!s.equals(other.s))
                return false;
            return true;
        }

        public PoJo(String s) {
            super();
            this.s = s;
        }

    }
    public static void test3() {
        Set<String>[] sets = new Set[10];
        for (int i = 0; i < 10; ++i) {
            sets[i] = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
            sets[i].add("Foo");
        }
        PoJo p = new PoJo("Foo");
        sets[5].remove(p); // <- bug
    }

    public static void test() {
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
        s.add("Foo");
        PoJo p = new PoJo("Foo");
        s.remove(p); // <- bug
    }
    public static void test2() {
        List<Set<String>> sets = new ArrayList<Set<String>>();
        for (int i = 0; i < 10; ++i) {
            sets.add( Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>()));
            sets.get(i).add("Foo");
        }
        PoJo p = new PoJo("Foo");
        sets.get(5).remove(p); // <- bug
    }
}
