package gcUnrelatedTypes;

import java.util.LinkedList;
import java.util.Vector;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class MoreChecks {
    @ExpectWarning("GC")
    public void test() {
        LinkedList<Integer> lst = new LinkedList<Integer>();
        lst.add(1);
        lst.add(2);
        lst.add(3);
        lst.removeFirstOccurrence("a");
        lst.removeLastOccurrence("a");
        Vector<Integer> v = new Vector<Integer>();
        v.addAll(lst);
        v.indexOf((long) 1, 1);

        v.lastIndexOf((long) 1, 1);
    }

}
