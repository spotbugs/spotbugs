package jsr305;

import java.lang.annotation.ElementType;

import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Foo(when=When.ALWAYS)
@TypeQualifierDefault(ElementType.PARAMETER)
public @interface DefaultFooParameters {

}
