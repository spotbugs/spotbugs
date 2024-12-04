package multithreaded.compoundoperation;

public class CompoundPostIncrementationOnSharedVariable extends Thread {
    private double num = 3.0;

    public double getNum() {
        return num;
    }

    public void toggle() {
        num++;
    }
}
