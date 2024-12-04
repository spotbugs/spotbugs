package multithreaded.compoundoperation;

public class CompoundDivisionOnVariable extends Thread {
    private double num = 3.0;

    public void toggle() {
        num /= 3.0;
    }
}
