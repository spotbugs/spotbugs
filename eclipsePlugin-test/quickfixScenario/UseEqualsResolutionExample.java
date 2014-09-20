
public class UseEqualsResolutionExample {

    public static void main(String[] args) {
        if (args[0] == "foo") {
            System.out.println("foo");
        }
    }

    protected boolean check(String s) {
        return "foo" == s;
    }

}
