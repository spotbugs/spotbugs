import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class ReflectionIncreaseAccessibilityTest {
    ReflectionIncreaseAccessibilityTest() {}

    @ExpectWarning("REFL")
    public static <T> T create(Class<T> c)
            throws InstantiationException, IllegalAccessException {
        return c.newInstance();
    }
}
