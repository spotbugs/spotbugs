package multithreaded.primitivewrite;

public class NotSynchronizedLong implements Runnable {
    private double d = 0.0;

    void setValue(double value) {
        d = value;
    }

    void printValue() {
        System.out.println(d);
    }

    @Override
    public void run() {
        printValue();
    }
}
