package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2387290 {
    public static void method1(Object obj) {

        byte[] b = { 12, 13, 24, 54 };
        obj = b;

        if (byte[].class.isInstance(obj)) {
            byte[] value = (byte[]) obj;
        }
    }

    @NoWarning("BC")
    public static void method2(Object obj) {

        if (byte[].class.isInstance(obj)) {
            byte[] value = (byte[]) obj;
        }
    }
}
