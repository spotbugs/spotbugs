package jsr305;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.GREEN, when = When.NEVER)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface NeverGreen {

}
