package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.QualifierNickname;
import javax.annotation.meta.When;

/** Used to annotate a value that may be negative; uses of it should check for negative values
 * before using it in a way that requires the value to be nonnegative */

@Documented
@QualifierNickname
@Nonnegative(when=When.MAYBE_NOT)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckForSigned {
    ElementType[] applyTo() default {};

}
