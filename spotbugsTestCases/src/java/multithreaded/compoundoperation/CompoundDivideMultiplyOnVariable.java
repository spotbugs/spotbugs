package multithreaded.compoundoperation;

public class CompoundDivideMultiplyOnVariable extends Thread {
    private double num = 3.0;

    public void divide() {
        num /= 3.0;
    }

    public void multiply() {
        num *= 3.0;
    }
}
