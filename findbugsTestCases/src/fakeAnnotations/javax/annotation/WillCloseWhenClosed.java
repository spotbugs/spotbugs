package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR,ElementType.TYPE,ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
/**
 *  Used to annotate a constructor/factory parameter to indicate that 
 *  returned object (X) will close the resource when X is closed.
 */
public @interface WillCloseWhenClosed {

}
