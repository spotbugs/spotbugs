
package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.*;
import java.lang.annotation.ElementType;
import static java.lang.annotation.ElementType.*;

@Documented
@Target({METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
/**
 * This annotation is used to denote a method whose return value
 * should always be checked when invoking the method.
 * 
 * The checker treats this annotation as inherited by overriding methods.
 */

public @interface CheckReturnValue {

    String priority() default "medium";
    /**
     * A textual explaination of why the return value should be checked
     */
    String explanation() default "";
    /**
     * A value indicating whether or not the return value should be checked.
     *
     * The only reason to use anything other than the default is if you are
     * overriding a method that notes that its return value should be checked,
     * but your return value doesn't need to be checked.
     */
    boolean check() default true;

}
