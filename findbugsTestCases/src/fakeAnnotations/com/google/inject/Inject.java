package com.google.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates members of your implementation class (constructors, methods and fields) into which the Injector should inject values
 *
 */
@Target(value={ElementType.METHOD,ElementType.CONSTRUCTOR,ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Inject {

}
