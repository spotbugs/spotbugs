/* ****************************************
 * $Id$
 * SF bug Bug2672946:
 *   Null pointer on some return path false positive
 *
 * JVM:  1.6.0 (OS X, x86)
 * FBv:  1.3.8-dev-20090309
 *
 * Test case based on example code from bug report
 * **************************************** */

package sfBugs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.meta.TypeQualifierDefault;

import jsr305.FieldsNonNullByDefault;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/* ********************
 * Behavior at filing:  false positive NP from getField()
 *   M D NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE NP: \
 *   Possible null pointer dereference in sfBugs.Bug2672946B.getField() \
 *   due to return value of called method \
 *   Returned at Bug2672946B.java:[line 30]
 *
 * Expected behavior:  no NP warning, parent class is annotated with @NonNull
 * ******************** */
@FieldsNonNullByDefault
@MethodsAreCheckNullByDefault
@ParametersAreNonnullByDefault



abstract class Bug2672946B extends Bug2672946A {
    public Bug2672946B(Bug2672946B field) {
        super(field);
    }

    /** This method will inherit the @Nonnull annotation from Bug2672946A, rather than use the class annotation @MethodsAreCheckNullByDefault. 
     * Thus, we do not expect a warning about relaxing an annotation */
    @NoWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
    @Override
    public Bug2672946B getField() {
        Bug2672946A field = super.getField();
        if (field.hashCode() == 42)
            System.out.println("Found it");
        return (Bug2672946B) field;
    }
    
    /** However, the explicit warning here does override the inherited annotation */
    @ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
    @CheckForNull
    @Override
    public Bug2672946B getField2() {
        Bug2672946A field = super.getField2();
        if (field.hashCode() == 42)
            System.out.println("Found it");
        return (Bug2672946B) field;
    }
    @ExpectWarning("NP")
    public Object foo() {
        if (bar().hashCode() == 17)
            return "x";
        return null;
    }
    
    @NoWarning("NP")
    public Object foo2() {
        if (getField().hashCode() == 17)
            return "x";
        return null;
    }
    @ExpectWarning("NP")
    public Object foo3() {
        if (bar2().hashCode() == 17)
            return "x";
        return null;
    }
    @ExpectWarning("NP")
    public Object foo4() {
        if (getField2().hashCode() == 17)
            return "x";
        return null;
    }
    abstract Object bar();
    
    abstract @CheckForNull Object bar2();
    
}

@CheckForNull
@TypeQualifierDefault(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MethodsAreCheckNullByDefault {

}
