public class NoNaNCompare {
    public static boolean checkIt(double d) { // This is a probably a bug
        return (d == Double.NaN);
    }

    public static boolean checkIt(float f) { // This is a probably a bug
        return (f == Float.NaN);
    }

    public static void main(String[] args) {
        double d = Double.NaN;
        System.out.println("d is a NaN: " + checkIt(d));
        float f = Float.NaN;
        System.out.println("f is a NaN: " + checkIt(f));
    }
}
