import edu.umd.cs.findbugs.annotations.NoWarning;

class SynchronizationTest6 {
    int x;

    Object lock = new Object();

    public void add1() {
        synchronized (lock) {
            x += 1;
        }
    }

    public void add2() {
        synchronized (lock) {
            x += 2;
        }
    }

    public void add3() {
        synchronized (lock) {
            x += 3;
        }
    }

    public void add4() {
        synchronized (lock) {
            x += 4;
        }
    }

    public void add5() {
        synchronized (lock) {
            x += 5;
        }
    }

    public void add6() {
        synchronized (lock) {
            x += 6;
        }
    }

    @NoWarning("IS2_INCONSISTENT_SYNC")
    public int get2X() {
        return x + x;
    }
}
