package multithreaded.compoundoperation;

public class CompoundRightShiftingOnSharedVariable {
    private int num = 1;

    public void toggle() {
        num >>= 3;
    }

    public double getNum() {
        return num;
    }

    public void print() {
        synchronized(CompoundRightShiftingOnSharedVariable.class) {
            System.out.println("synchronized block");
        }
    }
}
