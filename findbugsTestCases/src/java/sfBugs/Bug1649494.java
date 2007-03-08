package sfBugs;

import java.util.ArrayList;
import java.util.List;

public class Bug1649494 {
    public static <T>  ArrayList<T> f(T x) {
        ArrayList<T> lst = new ArrayList<T>();
        lst.add(x);
        lst.add(null);
        return lst;
    }
    public static <T>  void addNull(List<T> lst) {
         lst.add(null);

    }
}
