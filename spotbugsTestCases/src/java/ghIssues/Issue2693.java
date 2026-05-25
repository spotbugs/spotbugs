package ghIssues;

public class Issue2693 {

    public static void nullDerefInCatch() {
        String str = null;
        try {
            throw new NullPointerException();
        } catch (Exception e) {
            str.length();
        }
    }
}
