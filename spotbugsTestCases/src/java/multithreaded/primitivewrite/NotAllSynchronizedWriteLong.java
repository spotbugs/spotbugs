package multithreaded.primitivewrite;

public class NotAllSynchronizedWriteLong implements Runnable {
    private double d = 0.0;

    private void setValue(double value) {
        d = value;
    }

    void printValue() {
        System.out.println(d);
    }

    synchronized public void syncSet(double val) {
        setValue(val);
    }

    void nonsyncedSet(double val) {
        setValue(val);
    }

    @Override
    public void run() {
        // TODO
    }
}
