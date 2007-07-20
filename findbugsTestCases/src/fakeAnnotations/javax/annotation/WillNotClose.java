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
 *  Used to annotate a method parameter to indicate that 
 *  this method will not close the resource.
 */
public @interface WillNotClose {

}
