package jsr305;

import com.github.spotbugs.jsr305.annotation.meta.Exhaustive;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifier;
import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierNickname;
import com.github.spotbugs.jsr305.annotation.meta.When;

public @TypeQualifier
@interface Sign {
    @Exhaustive
    NumericSign value();

    When when() default When.ALWAYS;

    enum NumericSign {
        NEGATIVE, ZERO, POSITIVE
    };

    // Define more signess type qualifiers than we probably need
    @Sign(NumericSign.POSITIVE)
    @TypeQualifierNickname
    @interface Positive {
    }

    @Sign(NumericSign.NEGATIVE)
    @TypeQualifierNickname
    @interface Negative {
    }

    @Sign(value = NumericSign.NEGATIVE, when = When.NEVER)
    @TypeQualifierNickname
    @interface Nonnegative {
    }

    @Sign(value = NumericSign.NEGATIVE, when = When.MAYBE)
    @TypeQualifierNickname
    @interface MaybeNegative {
    }

    @Sign(value = NumericSign.ZERO, when = When.NEVER)
    @TypeQualifierNickname
    @interface Nonzero {
    }

    @Sign(value = NumericSign.POSITIVE, when = When.MAYBE)
    @MaybeNegative
    @TypeQualifierNickname
    @interface Signed {
    }

}
