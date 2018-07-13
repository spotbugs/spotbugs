package jsr305;

import java.lang.annotation.ElementType;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierDefault;
import com.github.spotbugs.jsr305.annotation.meta.When;

@Foo(when = When.ALWAYS)
@TypeQualifierDefault(ElementType.PARAMETER)
public @interface DefaultFooParameters {

}
