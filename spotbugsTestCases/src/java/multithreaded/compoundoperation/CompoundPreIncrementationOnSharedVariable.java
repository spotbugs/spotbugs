package multithreaded.compoundoperation;

public class CompoundPreIncrementationOnSharedVariable extends Thread {
    private double num = 3.0;

    public void toggle() {
        ++num;
    }

    public double getNum() {
        return num;
    }
}
