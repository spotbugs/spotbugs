package jsr305;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;

/**
 * This annotation does not specify a When value, so it must be checked
 * strictly.
 */
@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Strict {
}
