package multithreaded.compoundoperation;

public class NotRealCompoundOp {
    private volatile int number;

    public void setNumber(int newValue) {
        checkNumber(newValue);
        if (number != newValue) {
            throw new IllegalStateException("It's already set to " + number);
        }
        number = newValue;
    }

    private static void checkNumber(int value) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException();
        }
    }

    public int getNumber() {
        return number;
    }
}
