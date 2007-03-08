package sfBugs;

import java.util.Comparator;
import java.util.List;

public class Bug1487961 {
    static class DateMgr {
        String getName() {
            return "a";
        }
    }

    void sort(List<DateMgr>[] aDateMgr) {
        Comparator<List<DateMgr>> c = new Comparator<List<DateMgr>>() {
            public int compare(List<DateMgr> o1, List<DateMgr> o2) {
                String a = o1.get(0).getName();
                String b = o2.get(0).getName();

                return a.compareTo(b);
            }
        };

        java.util.Arrays.sort(aDateMgr, c);
    }
}
