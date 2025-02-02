package multithreaded.primitivewrite;

public class SynchronizedLong {
    private long l = 0;

    synchronized void setValue(long value) {
        l = value;
    }

    synchronized void printValue() {
        System.out.println(l);
    }
}
