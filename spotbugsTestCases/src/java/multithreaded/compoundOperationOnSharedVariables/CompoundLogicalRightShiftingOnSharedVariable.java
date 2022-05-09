package multithreaded.compoundOperationOnSharedVariables;

public class CompoundLogicalRightShiftingOnSharedVariable extends Thread {
    private int num = 1;

    public void toggle() {
        num >>>= 3;
    }

    public double getNum() {
        return num;
    }
}
