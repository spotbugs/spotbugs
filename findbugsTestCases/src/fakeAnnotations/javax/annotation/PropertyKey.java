package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.When;

@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyKey {
	When when() default When.ALWAYS;
	ElementType[] applyTo() default {};
}
