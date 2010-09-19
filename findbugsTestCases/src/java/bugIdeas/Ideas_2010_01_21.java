package bugIdeas;

import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2010_01_21<T extends Map> {

    @ExpectWarning("GC")
    public boolean test(T t) {
        Set s = t.entrySet();
        return s.contains(5);
    }

    volatile int x;

    volatile long y;

    public void volatileIncrement() {
        x++;
    }

    public void volatileDecrement() {
        x--;
    }

    public void volatileIncrementLong() {
        y++;
    }

    public void volatileDecrementLong() {
        y--;
    }

}
