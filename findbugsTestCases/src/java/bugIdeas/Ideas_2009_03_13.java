package bugIdeas;

import java.util.HashMap;
import java.util.Map;

public class Ideas_2009_03_13 {

    static void bar(Object o) {
        if (o == null)
            throw new NullPointerException();
		return;
    }

    static int foo(Object x) {
        bar(x);
        if (x == null)
			return x.hashCode();
        return 42;
    }

    static int foo2(Object x) {
        if (x == null) {
            bar(x);
			return x.hashCode();
        }
        return 42;
    }
	
    Map<Integer,Integer> map = new HashMap<Integer,Integer>();

    int noisyBug(String x) {
		return map.get(x);
    }
    Integer silentBug(String x) {
        return map.get(x);
	}

}
