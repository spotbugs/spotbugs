package multithreaded.compoundoperation;

public class CompoundPreDecrementationOnSharedVariable extends Thread {
    private double num = 3.0;

    public void toggle() {
        --num;
    }

    public double getNum() {
        return num;
    }
}
