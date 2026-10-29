package ghIssues;

public class Issue4407 {

    public static void main(String[] args) {
        for (double d = -10.0; d < 100.0; ++d) {
            System.out.println(d);
        }

        int d = 0;
        while (d < 100.0) {
            System.out.println(d);
            d += 10.0;
        }
    }
}
