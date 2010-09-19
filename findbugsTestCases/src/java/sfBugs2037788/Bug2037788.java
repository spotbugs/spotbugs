package sfBugs2037788;

public class Bug2037788 {
    public static void main(String args[]) {
        System.out.println("This is a test.");
        // Next line should generate findbugs warning
        System.exit(0);
    }
}
