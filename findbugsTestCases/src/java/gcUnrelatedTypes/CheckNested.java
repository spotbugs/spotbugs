package gcUnrelatedTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckNested {

    public static void main(String args[]) {
        Set<Set<Integer>> s = new HashSet<Set<Integer>>();
        Map<Integer, Set<Integer>> m = new HashMap<Integer, Set<Integer>>();

        Set<Long> l = new HashSet<Long>();

        s.remove(l);
        m.get(5).remove(l);

    }

}
