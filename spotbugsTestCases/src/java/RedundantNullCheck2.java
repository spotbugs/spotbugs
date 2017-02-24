import java.util.HashMap;
import java.util.Map;

class RedundantNullCheck2 {
    Map m = new HashMap();

    public int f(Object x) {

        Object y = m.get(x);

        if (y == null)
            return 0;

        String z = (String) y;
        try {
            if (z.hashCode() == 17)
                return 42;
            return z.hashCode();
        } catch (RuntimeException e) {

            if (z.hashCode() == 42)
                System.out.println(42);
            throw e;
        }
    }
}
