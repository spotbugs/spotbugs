package org.immutables.value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Generated {

    String from() default "";

    String generator() default "";
}
