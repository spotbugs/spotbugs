package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TestContainsAllAndRemoveAll {

    public static void main(String args[]) {
        Set<Integer> i = new HashSet<Integer>();
        Set<String> s = new HashSet<String>();
        i.removeAll(s);
        i.containsAll(s);

        falsePositive();
    }

    public static void falsePositive() {
        Set<Integer> i = new HashSet<Integer>();
        List<Integer> s = new LinkedList<Integer>();
        i.removeAll(s);
        i.containsAll(s);
    }

}
