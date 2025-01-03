package multithreaded.primitivewrite;

public class NonSynchronizedWriteLong implements Runnable {
    private double d = 0.0;

    void setValue(double value) {
        d = value;
    }

    void printValue() {
        System.out.println(d);
    }

    @Override
    public void run() {
        synchronized (NonSynchronizedWriteLong.class) {
            printValue();
        }
    }
}
