package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2776701 implements Comparable<Bug2776701> {

    short x;

    @Override
    public int hashCode() {
        return x;
    }

    @NoWarning("EQ_UNUSUAL")
    @Override
    public boolean equals(Object o) {
        if (o instanceof Bug2776701)
            return compareTo((Bug2776701) o) == 0;
        return false;
    }

    @Override
    public int compareTo(Bug2776701 o) {
        return this.x - o.x;
    }

}
