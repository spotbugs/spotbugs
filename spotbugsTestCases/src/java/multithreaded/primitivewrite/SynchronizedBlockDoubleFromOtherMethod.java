package multithreaded.primitivewrite;

public class SynchronizedBlockDoubleFromOtherMethod {
    private double d = 0.0;

    void setValue(double value) {
        synchronized (SynchronizedBlockDoubleFromOtherMethod.class) {
            d = value;
        }
    }

    private void printValue() {
        System.out.println(d);
    }

    synchronized void synchronizedLog() {
        printValue();
    }

    public void anotherSyncPrint() {
        synchronized (SynchronizedBlockDouble.class) {
            printValue();
        }
    }
}
