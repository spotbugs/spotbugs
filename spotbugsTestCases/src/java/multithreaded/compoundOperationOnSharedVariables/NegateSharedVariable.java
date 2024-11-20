package multithreaded.compoundOperationOnSharedVariables;

public class NegateSharedVariable extends Thread {
    private int num = 3;

    public void toggle() {
        num = -num;
    }

    public Integer getNum() {
        return num;
    }
}
