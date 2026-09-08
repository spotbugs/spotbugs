package multithreaded.compoundoperation;

public class CompoundIANDOperationOnSharedVariable {
    private int num = 1;
    private volatile int volatileField; // just to signal that its multithreaded

    public void toggle() {
        num &= 3;
    }

    public double getNum() {
        return num;
    }
}
