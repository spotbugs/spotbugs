package bugIdeas;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.ResultSet;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierNickname;
import com.github.spotbugs.jsr305.annotation.meta.When;

public class Ideas_2013_01_29 {

    @Documented
    @TypeQualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {
        When when() default When.ALWAYS;
    }

    @Foo(when = When.NEVER)
    @TypeQualifierNickname
    public @interface NotFoo { }

    @Foo(when = When.MAYBE)
    @TypeQualifierNickname
    public @interface MaybeFoo { }

    int foobar() {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @NotFoo Object test1(@Foo Object x) {
        return x;
    }

    @Foo Object test2(@MaybeFoo Object x, boolean b) {
        if (b) System.out.println("b");
        return x;
    }
    @Foo Object test2(@MaybeFoo Object x, @Foo Object y, boolean b) {
        if (b) return y;
        return x;
    }

    @Nonnull Object testNullness(@CheckForNull Object x, boolean b) {
        if (b)
            System.out.println("b");
        return x;
    }
    @Nonnull Object testNullness(@CheckForNull Object x, @Foo Object y, boolean b) {
        if (b) return y;
        return x;
    }


}
