package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.QualifierChecker;
import javax.annotation.meta.When;

@Documented
@Qualifier(applicableTo=String.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pattern {
	@RegEx String value();
    ElementType[] applyTo() default {};
	
	static class Checker implements QualifierChecker<Pattern> {

		public When forConstantValue(Pattern annotation, Object value) {
			if (java.util.regex.Pattern.matches(annotation.value(), (String)value))
				return When.ALWAYS;
			return When.MAYBE_NOT;
		}
		
	}
}
