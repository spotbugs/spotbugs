package sfBugs;

import java.util.Comparator;

public class Bug2832071 {

    class CompareIntegers implements Comparator<Integer> {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o2.compareTo(o1);
        }
    }

}
