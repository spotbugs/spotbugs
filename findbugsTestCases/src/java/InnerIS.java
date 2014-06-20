import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class InnerIS {
    private int x;

    class Inner {
        @ExpectWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
        public int f() {
            synchronized (InnerIS.this) {
                return x;
            }
        }
    }

    public synchronized int get() {
        return x;
    }

    public synchronized void set(int v) {
        x = v;
    }

}

// vim:ts=4
