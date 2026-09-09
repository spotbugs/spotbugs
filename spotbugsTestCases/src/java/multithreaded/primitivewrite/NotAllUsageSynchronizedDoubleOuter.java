package multithreaded.primitivewrite;

public class NotAllUsageSynchronizedDoubleOuter {
    private double d = 0.0;

    void setValue(double value) {
        d = value;
    }

    private void printValue() {
        System.out.println(d);
    }

    synchronized void synchronizedLog() {
        printValue();
    }

    public void nonsynchronizedPrint() {
        printValue();
    }
}
