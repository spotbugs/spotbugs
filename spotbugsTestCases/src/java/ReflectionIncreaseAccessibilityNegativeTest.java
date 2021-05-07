import edu.umd.cs.findbugs.annotations.NoWarning;

public class ReflectionIncreaseAccessibilityNegativeTest {
    ReflectionIncreaseAccessibilityNegativeTest() {}

    @NoWarning("REFL")
    public static <T> T create(Class<T> c)
            throws InstantiationException, IllegalAccessException {
        if (c.getConstructors().length == 0) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPackageAccess("");
            }
        }
        return c.newInstance();
    }
}
