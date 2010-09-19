import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BadStatic {

    static String name;

    @ExpectWarning("ST")
    public BadStatic(String n) {
        name = n;
    }
}
