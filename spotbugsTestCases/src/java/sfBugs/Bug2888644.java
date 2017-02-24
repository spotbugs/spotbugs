package sfBugs;

public class Bug2888644 {
    private static String assignMe = null;

    public void bug1() {
        assignMe = "OK";
    }

    public void bug2() {
        if (assignMe.contains("OK")) {
            System.out.println("OK!");
        }
        assignMe = "OK";
    }
}
