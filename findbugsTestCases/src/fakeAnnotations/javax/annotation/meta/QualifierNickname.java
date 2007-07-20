package javax.annotation.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 
 * This annotation is applied to a annotation, and marks the annotation as being
 * a qualifier nickname. Applying a nickname annotation X to a element Y should
 * be interpreted as having the same meaning as applying all of annotations of X
 * (other than QualifierNickname) to Y.
 * 
 * Thus, you might define a qualifier SocialSecurityNumber as follows:
 * 
 * 
 * <code>
 @Documented
 @QualifierNickname @Pattern("[0-9]{3}-[0-9]{2}-[0-9]{4}") 
 @Retention(RetentionPolicy.RUNTIME)
 public @interface SocialSecurityNumber {
 ElementType[] applyTo() default {};
 }
 </code>
 * 
 * 
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface QualifierNickname {

}
