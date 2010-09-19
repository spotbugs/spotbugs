import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BadConstructor {
    int x;

    @ExpectWarning("Nm")
    public void BadConstructor() {
        x = 17;
    }
}
