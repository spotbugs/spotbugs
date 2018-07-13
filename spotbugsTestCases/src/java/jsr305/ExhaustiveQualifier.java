package jsr305;

import com.github.spotbugs.jsr305.annotation.meta.Exhaustive;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.When;

public @TypeQualifier
@interface ExhaustiveQualifier {
    enum Color {
        RED, GREEN, BLUE
    };

    When when() default When.ALWAYS;

    @Exhaustive
    Color value();
}
