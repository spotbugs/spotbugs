package multithreaded.primitivewrite;

public class VolatileLong {
    private volatile long l = 0;

    void setValue(long value) {
        l = value;
    }

    void printValue() {
        System.out.println(l);
    }
}
