package jsr305;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.When;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Foo {
    When when() default When.ALWAYS;

    ElementType[] defaultFor() default {};
}
