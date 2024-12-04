package multithreaded.primitivewrite;

public class NotAllUsageSynchronizedDouble {
    private double d = 0.0;

    void setValue(double value) {
        synchronized (NotAllUsageSynchronizedDouble.class) {
            d = value;
        }
    }

    void printValue() {
        System.out.println(d);
    }
}
