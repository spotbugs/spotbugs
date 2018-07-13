package jsr305;

import com.github.spotbugs.jsr305.annotation.meta.Exclusive;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.When;

public @TypeQualifier
@interface ExclusiveQualifier {
    enum Color {
        RED, GREEN, BLUE
    };

    When when() default When.ALWAYS;

    @Exclusive
    Color value();
}
