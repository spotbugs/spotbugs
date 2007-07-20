package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.When;

/**
 * This annotation a value that is of a particular syntax, such as Java syntax
 * or regular expression syntax. This can be used to provide syntax checking of
 * constant values at compile time, run time checking at runtime, and can assist
 * IDEs in deciding how to interpret String constants (e.g., should a refactoring
 * that renames method x() to y() update the String constant "x()"). 
 * 
 *
 */
@Documented
@Qualifier(applicableTo=String.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Syntax {
    /** Value indicating the particular syntax denoted by this annotation.
     * Different tools will recognize different syntaxes, but some
     * proposed canonical values are:
     * <ul>
     * <li> "Java"
     * <li> "RegEx"
     * <li> "JavaScript"
     * <li> "Ruby"
     * <li> "Groovy"
     * <li> "SQL"
     * <li> "FormatString"
     * </ul>
     * 
     * Syntax names can be followed by a colon and a list of key value pairs, separated by commas.
     * For example, "SQL:dialect=Oracle,version=2.3". Tools should ignore any keys they don't 
     * recognize.
     */
	String value();
    When when() default When.ALWAYS;
    ElementType[] applyTo() default {};
}
