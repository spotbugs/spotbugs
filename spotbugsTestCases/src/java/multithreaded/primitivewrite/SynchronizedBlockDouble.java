package multithreaded.primitivewrite;

public class SynchronizedBlockDouble {
    private double d = 0.0;

    void setValue(double value) {
        synchronized (SynchronizedBlockDouble.class) {
            d = value;
        }
    }

    void printValue() {
        synchronized (SynchronizedBlockDouble.class) {
            System.out.println(d);
        }
    }
}
