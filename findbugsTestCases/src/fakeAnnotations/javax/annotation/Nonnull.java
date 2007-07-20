package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.When;

@Documented
@Qualifier(applicableTo=Object.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
	When when() default When.ALWAYS;
	ElementType[] applyTo() default {};
}
