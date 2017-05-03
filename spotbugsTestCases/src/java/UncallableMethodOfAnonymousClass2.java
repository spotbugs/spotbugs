import java.util.Comparator;
import java.util.TreeMap;

public class UncallableMethodOfAnonymousClass2 {

    private final static Comparator COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            int result = o1.hashCode() - o2.hashCode();
            assert (result > 0);
            return result;
        }
    };

    TreeMap getMap() {
        return new TreeMap(COMPARATOR);
    }

}
