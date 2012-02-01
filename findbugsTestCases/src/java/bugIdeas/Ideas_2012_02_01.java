package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2012_02_01 {

    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {
    }

    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
    }

    
    static  @Foo int getFoo() {
        return 42;
    }
    @ExpectWarning("TQ")
    Ideas_2012_02_01(int x, @Foo int foo) {
        this(x, foo, "x");
    }
    @ExpectWarning("TQ")
    Ideas_2012_02_01(int x, StringBuffer b) {
        this(x, getFoo(), b.toString());
    }

    Ideas_2012_02_01(int x, @Bar int bar, String s) {
    }

    @ExpectWarning("TQ")
    Ideas_2012_02_01(@Foo int foo) {
        this(foo, "x");
    }

    Ideas_2012_02_01(@Bar int bar, String s) {
    }

}
