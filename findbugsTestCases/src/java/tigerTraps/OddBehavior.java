package tigerTraps;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class OddBehavior {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(-2, -1, 0, 1, 2);

        boolean foundOdd = false;
        for (Iterator<Integer> it = list.iterator(); it.hasNext();)
            foundOdd = foundOdd || isOdd(it.next());

        System.out.println(foundOdd);
    }

    private static boolean isOdd(int i) {
        return (i & 1) != 0;
    }
}
