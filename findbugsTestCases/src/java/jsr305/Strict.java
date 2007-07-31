package jsr305;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

/**
 * This annotation does not specify a When value,
 * so it must be checked strictly.
 */
@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Strict {
	ElementType[] applyTo() default {};
}
