package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature328 {
    public static class FloatHolder implements Comparable<FloatHolder> {
        float d;
        
        public FloatHolder(float d) {
            this.d = d;
        }

        @Override
        @ExpectWarning("CO_COMPARETO_INCORRECT_FLOATING")
        public int compareTo(FloatHolder o) {
            float d1 = d;
            float d2 = o.d;
            return d1 > d2 ? 1: d1 == d2 ? 0 : -1;
        }
    }

    public static class FloatHolder2 implements Comparable<FloatHolder2> {
        float d;
        
        public FloatHolder2(float d) {
            this.d = d;
        }

        @Override
        @ExpectWarning("CO_COMPARETO_INCORRECT_FLOATING")
        public int compareTo(FloatHolder2 o) {
            float diff = d - o.d;
            return diff > 0 ? 1: diff < 0 ? 0 : -1;
        }
    }

    public static class FloatHolderOk implements Comparable<FloatHolderOk> {
        float d;
        
        public FloatHolderOk(float d) {
            this.d = d;
        }

        @Override
        @NoWarning("CO_COMPARETO_INCORRECT_FLOATING")
        public int compareTo(FloatHolderOk o) {
            return Float.compare(d, o.d);
        }
    }

    public static class DoubleHolder implements Comparable<DoubleHolder> {
        double d;
        
        public DoubleHolder(double d) {
            this.d = d;
        }

        @Override
        @ExpectWarning("CO_COMPARETO_INCORRECT_FLOATING")
        public int compareTo(DoubleHolder o) {
            double d1 = d;
            double d2 = o.d;
            return d1 > d2 ? 1: d1 == d2 ? 0 : -1;
        }
    }

    public static class DoubleHolderOk implements Comparable<DoubleHolderOk> {
        double d;
        
        public DoubleHolderOk(double d) {
            this.d = d;
        }

        @Override
        @NoWarning("CO_COMPARETO_INCORRECT_FLOATING")
        public int compareTo(DoubleHolderOk o) {
            return Double.compare(d, o.d);
        }
    }
}
