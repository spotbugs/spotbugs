package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.QualifierNickname;
import javax.annotation.meta.When;


/** Used to annotate a value of unknown sign */

@Documented
@QualifierNickname
@Nonnegative(when=When.UNKNOWN)
@Retention(RetentionPolicy.RUNTIME)
public @interface Signed {
    ElementType[] applyTo() default {};

}
