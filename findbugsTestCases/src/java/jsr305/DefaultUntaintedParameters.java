package jsr305;

import java.lang.annotation.ElementType;
import javax.annotation.Untainted;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Untainted(when=When.ALWAYS)
@TypeQualifierDefault(ElementType.PARAMETER)
public @interface DefaultUntaintedParameters {

}
