package sfBugs;

public class Bug2821733 {

    protected static final int objectCompLowPriority(Comparable c1, Comparable c2) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null && c2 != null) {
            return -1;
        } else if (c1 != null && c2 == null) {
            return 1;
        } else {
            return c1.compareTo(c2);
        }
    }

    protected static final int objectCompFalsePositive(Comparable c1, Comparable c2) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return -1;
        } else if (c2 == null) {
            return 1;
        } else {
            return c1.compareTo(c2);
        }
    }

}
