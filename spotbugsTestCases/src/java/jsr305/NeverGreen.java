package jsr305;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierNickname;
import com.github.spotbugs.jsr305.annotation.meta.When;

@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.GREEN, when = When.NEVER)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface NeverGreen {

}
