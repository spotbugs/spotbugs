package npe;

public class ZeroTrip {

    public static int nonNullLoop(String[] args) {
        String ret = null;
        for (String s : args) {
            ret = s;
        }
        return ret.hashCode();
    }

}
