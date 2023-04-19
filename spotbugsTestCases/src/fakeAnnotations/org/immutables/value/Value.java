package org.immutables.value;

import java.lang.annotation.*;

public @interface Value {

    @Documented
    @Target(ElementType.TYPE)
    @interface Immutable {

        boolean singleton() default false;

        boolean intern() default false;

        boolean copy() default true;

        boolean prehash() default false;

        boolean lazyhash() default false;

        boolean builder() default true;
    }
}
