package npe;

public class DetectThrowers {

    private void oops(String s) {
        throw new RuntimeException(s);
    }

    public int falsePositive(Object x) {
        if (x == null)
            oops("x is null");
        return x.hashCode();
    }

}
