package sfBugsNew;

import java.io.PrintStream;
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

    // Bug #1282: No warning should be generated if only static methods are synthetic
    @NoWarning("ISC_INSTANTIATE_STATIC_CLASS")
    public void test3() {
        final Property3 p = new Property3();
       // compiler generates: static synthetic access$0(Bug1282$Property3, String) : void
        p.name = "test";

        final PrintStream out = System.out;
        // compiler generates: static synthetic access$1(Bug1282$Property3) : String
        out.println(p.name);
    }

    static class Property3 {
        private String name;
    }
}
