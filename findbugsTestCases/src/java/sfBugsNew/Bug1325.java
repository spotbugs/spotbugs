package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1325<P extends java.io.Serializable & Comparable<P>> implements java.io.Serializable, Comparable<Bug1325<P>> {
    private static final long serialVersionUID = 1L;

    private final P startPoint;

    private final P endPoint;

    @NoWarning("BC_UNCONFIRMED_CAST")
    public Bug1325(P start, P end) {
        startPoint = start;
        endPoint = end;
        if (start.compareTo(end) > 0)
            throw new IllegalArgumentException("start after end");
    }

    @Override
    public int compareTo(Bug1325<P> o) {
        final int comp = startPoint.compareTo(o.startPoint);
        return comp != 0 ? comp : endPoint.compareTo(o.endPoint);
    }

    @Override
    public int hashCode() {
        return (31 + (endPoint == null ? 0 : endPoint.hashCode())) * 31 + (startPoint == null ? 0 : startPoint.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Bug1325 && startPoint.equals(((Bug1325) obj).startPoint)
                && endPoint.equals(((Bug1325) obj).endPoint);
    }

    public static class Alt<P extends Comparable<P> & java.io.Serializable> implements java.io.Serializable,
            Comparable<Alt<P>> {
        private static final long serialVersionUID = 1L;

        private final P startPoint;

        private final P endPoint;

        @NoWarning("BC_UNCONFIRMED_CAST")
        public Alt(P start, P end) {
            startPoint = start;
            endPoint = end;
            if (start.compareTo(end) > 0)
                throw new IllegalArgumentException("start after end");
        }

        @Override
        public int compareTo(Alt<P> o) {
            final int comp = startPoint.compareTo(o.startPoint);
            return comp != 0 ? comp : endPoint.compareTo(o.endPoint);
        }

        @Override
        public int hashCode() {
            return (31 + (endPoint == null ? 0 : endPoint.hashCode())) * 31 + (startPoint == null ? 0 : startPoint.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Alt && startPoint.equals(((Alt) obj).startPoint) && endPoint.equals(((Alt) obj).endPoint);
        }
    }
}
