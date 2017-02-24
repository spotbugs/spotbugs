package sfBugs;

import java.util.ArrayList;
import java.util.List;

public class Bug1649494 {
    public static ArrayList f(Object x) {
        ArrayList lst = new ArrayList();
        lst.add(x);
        lst.add(null);
        return lst;
    }

    public static void addNull(List lst) {
        lst.add(null);

    }
}
