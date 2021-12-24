import java.util.Comparator;
import java.util.concurrent.Executors;

// Shouldn't report any bugs. This class is only to test JdtUtil.
public class C {
    public static class D {
    };

    public class E {
        public int compare(E other) {
            Comparator<E> comparator = new Comparator<E>() {

                public int compare(E o1, E o2) {
                    return 0;
                }
            };
            return comparator.compare(this, other);
        }

        @Override
        public String toString() {
            return C.this.toString();
        }
    };

    public void startAsyncTask() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
            }
        });
    }

    public Cloneable getCloneable() {
        return new Cloneable() {
            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
        };
    }
}
