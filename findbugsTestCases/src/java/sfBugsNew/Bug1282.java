package sfBugsNew;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.xml.crypto.Data;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1282 {

    @NoWarning("ISC_INSTANTIATE_STATIC_CLASS")
    public void test1() {
        final Property data = new Property();
        System.out.println(data);
    }

    static class Property {
        private String name;

        private Method getter;

        private Method setter;

        private Type type;
    }

    @ExpectWarning("ISC_INSTANTIATE_STATIC_CLASS")
    public void test2() {
        final Property2 data = new Property2();
        System.out.println(data);
    }

    static class Property2 {
        private static String name;

        private Method getter;

        private Method setter;

        private Type type;
    }
}
