package jsr305;

import javax.annotation.meta.Exhaustive;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

public @TypeQualifier @interface ExhaustiveQualifier {
	enum Color { RED, GREEN, BLUE };
	When when() default When.ALWAYS;
	@Exhaustive Color value();
}
