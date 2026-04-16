package org.immutables.value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
// X suffix so that this is not matched by the built-in filter for all "Generated" annotations
public @interface GeneratedX {

    String from() default "";

    String generator() default "";
}
