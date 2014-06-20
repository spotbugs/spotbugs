import java.util.Comparator;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class UncallableMethodOfAnonymousClass {
    private final static Comparator COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            int result = o1.hashCode() - o2.hashCode();
            assert (result > 0);
            return result;
        }
    };

    private class DepFactory {

        public Object getDep() {
            return new Object() {
                @NoWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
                public UncallableMethodOfAnonymousClass getDepSetter() {
                    return UncallableMethodOfAnonymousClass.this;
                }
            };
        }
    }

}
