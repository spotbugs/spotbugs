package sfBugs;

public class Bug2786541 {

    public static void isZero(Long val) {
        if (val == 0) {
            System.out.println("val == 0");
        }
    }
}
