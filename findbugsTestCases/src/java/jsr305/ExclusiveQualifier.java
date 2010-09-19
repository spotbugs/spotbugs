package jsr305;

import javax.annotation.meta.Exclusive;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

public @TypeQualifier
@interface ExclusiveQualifier {
    enum Color {
        RED, GREEN, BLUE
    };

    When when() default When.ALWAYS;

    @Exclusive
    Color value();
}
