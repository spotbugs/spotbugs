public class EmptyIfStatement {
    public static void main(String[] argv) {
        if (argv.length == 1)
            ;
        System.out.println("Hello, " + argv[0]);
    }
}
