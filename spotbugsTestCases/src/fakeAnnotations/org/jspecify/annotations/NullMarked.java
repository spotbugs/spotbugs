package org.jspecify.annotations;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stub for org.jspecify.annotations.NullMarked, used in SpotBugs test cases.
 */
@Documented
@Retention(RUNTIME)
@Target({CONSTRUCTOR, METHOD, MODULE, PACKAGE, TYPE})
public @interface NullMarked {
}
