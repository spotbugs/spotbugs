package sfBugsNew;

// Description.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.umd.cs.findbugs.annotations.NoWarning;

@NoWarning("USM_USELESS_SUBCLASS_METHOD")
public class Bug1206 {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    @interface Description {
        String value();
    }

    // A.java
    class A {
        @Description("Something A")
        int getSomething() {
            return 1;
        }
    }

    // B.java
    class B extends A {
        @Description("Something B")
        int getSomething() {
            return super.getSomething();
        }
    }
}
