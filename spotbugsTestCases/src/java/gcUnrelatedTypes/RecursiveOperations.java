package gcUnrelatedTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class RecursiveOperations {

    @ExpectWarning("DMI")
    public static void main(String args[]) {
        Set s = new HashSet();
        s.contains(s);
        s.remove(s);
        s.removeAll(s);
        s.retainAll(s);
        s.containsAll(s);

        Map m = new HashMap();
        m.get(m);
        m.remove(m);
        m.containsKey(m);
        m.containsValue(m);

        List lst = new LinkedList();
        lst.indexOf(lst);
        lst.lastIndexOf(lst);

    }

}
