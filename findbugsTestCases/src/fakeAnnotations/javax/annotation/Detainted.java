package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.QualifierNickname;
import javax.annotation.meta.When;


@Documented
@QualifierNickname
@Untainted(when=When.ASSUME_ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
public @interface Detainted {
    ElementType[] applyTo() default {};

}
