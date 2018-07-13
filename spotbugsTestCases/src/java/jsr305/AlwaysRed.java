package jsr305;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierNickname;
import com.github.spotbugs.jsr305.annotation.meta.When;

@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.RED, when = When.ALWAYS)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface AlwaysRed {

}
