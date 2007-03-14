package sfBugs;

public class Bug1678226 {
    public static final double CM_PER_INCH = 2.54;

    public static void main(String[] args) {
        double input = Double.parseDouble(args[0]);
        double inch = input / CM_PER_INCH;
        System.out.println("Input value(cm):" + input);
        System.out.println("Input value(inch):" + inch);
        if (inch == 55) {
            System.out.println("It is exactly 55 inches.");
        }
    }
}