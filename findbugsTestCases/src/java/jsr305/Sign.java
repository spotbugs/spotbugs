package jsr305;

import javax.annotation.meta.Exhaustive;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

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
