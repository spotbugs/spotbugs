package jsr305;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.RED, when = When.ALWAYS)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface AlwaysRed {

}
