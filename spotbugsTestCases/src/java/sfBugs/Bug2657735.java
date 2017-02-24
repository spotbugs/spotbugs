package sfBugs;

public class Bug2657735 {

    public static void main(String arg[]) {
        for (long i = -1; i > -3; i--) {
            System.out.println(i);
        }
    }

}
