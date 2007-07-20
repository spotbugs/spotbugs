package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.QualifierNickname;
import javax.annotation.meta.When;


@Documented
@QualifierNickname
@Untainted(when=When.MAYBE_NOT)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tainted {
    ElementType[] applyTo() default {};

}
