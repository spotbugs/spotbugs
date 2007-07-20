package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.QualifierChecker;
import javax.annotation.meta.When;

/** Used to annotate a value that should only contain nonnegative values */
@Documented
@Qualifier(applicableTo=Number.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnegative {
	When when() default When.ALWAYS;
	ElementType[] applyTo() default {};
	class Checker implements QualifierChecker<Nonnegative> {

		public When forConstantValue(Nonnegative annotation, Object v) {
			if (!(v instanceof Number)) return When.NEVER;
			boolean isNegative;
			Number value = (Number) v;
			if (value instanceof Long) 
				isNegative = value.longValue() < 0;
			else if (value instanceof Double) 
				isNegative = value.doubleValue() < 0;
			else if (value instanceof Float) 
				isNegative = value.floatValue() < 0;
			else
				isNegative = value.intValue() <  0;
			
			if (isNegative) return When.NEVER;
			else return When.ALWAYS;
			

		}
	}
}
