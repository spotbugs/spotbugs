package jsr305;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.When;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Bar {
    When when() default When.ALWAYS;

    int iField() default 1;

    char cField() default 'Z';

    String[] strArrField() default { "Blat", "Thud" };

    When[] eArrField() default { When.ALWAYS, When.MAYBE };
}
