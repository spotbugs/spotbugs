import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class ReflectionIncreaseAccessibilityNegativeTest {
    private Class cls;

    ReflectionIncreaseAccessibilityNegativeTest(Class cls) {
        this.cls = cls;
    }

    @NoWarning("REFLC")
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

    private static class C {
        Class<?> cls;
        public C(Class<?> cls) {
            this.cls = cls;
        }
    }

    @NoWarning("REFLC")
    public void strangeFP()
            throws InstantiationException, IllegalAccessException {
        cls.newInstance();
    }

    private C getC() {
        return new C(Integer.class);
    }

    @NoWarning("REFLC")
    public void strangeFP2()
            throws InstantiationException, IllegalAccessException {
        C c = getC();
        c.cls.newInstance();
    }
}
