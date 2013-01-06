package org.eclipse.jdt.annotation;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ PACKAGE, TYPE, METHOD, CONSTRUCTOR })
public @interface NonNullByDefault {
	boolean value() default true;
}
