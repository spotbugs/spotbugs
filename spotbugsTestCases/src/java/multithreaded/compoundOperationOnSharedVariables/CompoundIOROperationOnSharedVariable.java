package multithreaded.compoundOperationOnSharedVariables;

public class CompoundIOROperationOnSharedVariable extends Thread {
    private int num = 1;

    public void toggle() {
        num |= 3;
    }

    public double getNum() {
        return num;
    }
}
