package ghIssues.issue3396;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Generated {

    String from() default "";

    String generator() default "";
}
