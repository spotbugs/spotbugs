package multithreaded.compoundoperation;

public class CompoundLeftShiftingOnSharedVariable {
    private int num = 1;

    public void toggle() {
        num <<= 3;
    }

    public int getNum() {
        return num;
    }

    public synchronized void print() {
        System.out.println("synchronized method");
    }
}
