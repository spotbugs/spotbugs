package multithreaded.compoundoperation;

public class CompoundDivisionOnSharedVariable extends Thread {
    private double num = 3.0;

    public void toggle() {
        num /= 3.0;
    }

    public double getNum() {
        return num;
    }
}
