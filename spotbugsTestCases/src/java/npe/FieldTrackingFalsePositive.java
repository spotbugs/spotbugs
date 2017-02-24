package npe;

public class FieldTrackingFalsePositive {
    private Object x;

    private void init() {
        x = new Object();
    }

    public int getValue() {
        if (x == null)
            init();
        return x.hashCode();
    }

}
