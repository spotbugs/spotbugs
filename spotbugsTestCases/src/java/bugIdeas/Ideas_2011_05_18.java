package bugIdeas;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_05_18 implements Comparable<Ideas_2011_05_18> {


     public Ideas_2011_05_18(int x) {
        super();
        this.x = x;
    }

    int x;
    @Override
    public int compareTo(Ideas_2011_05_18 that) {
        if (this.x < that.x)
            return -1;
        else if (this.x > that.x)
            return 1;
        else
            return 0;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Ideas_2011_05_18))
            return false;
        return this.x == ((Ideas_2011_05_18)o).x;
    }

    @NoWarning("HE_USE_OF_UNHASHABLE_CLASS")
    public static void main(String args[]) {
        Ideas_2011_05_18 five = new Ideas_2011_05_18(5);
        Ideas_2011_05_18 six = new Ideas_2011_05_18(6);
        Set<Ideas_2011_05_18> s = new TreeSet<Ideas_2011_05_18>();
        s.add(five);
        s.add(six);
        Map<Ideas_2011_05_18,Integer> m = new TreeMap<Ideas_2011_05_18,Integer>();
        m.put(five, 5);
        m.put(six,6);

    }

}
