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
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

import jsr305.FieldsNonNullByDefault;

import edu.umd.cs.findbugs.annotations.DesireWarning;
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
@NoWarning("NP")
@FieldsNonNullByDefault
@MethodsAreCheckNullByDefault
@ParametersAreNonnullByDefault
// see comment in CheckRelaxingNullnessAnnotation.DetectorNode
// we should flag the entire class as suspicious it relaxes parent contract.
// Here the parent contract to never return null, but we relax it by applying "check for null"
@DesireWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
class Bug2672946B extends Bug2672946A {
    public Bug2672946B(Bug2672946B field) {
        super(field);
    }

    @Override
    public Bug2672946B getField() {
        return (Bug2672946B) super.getField();
    }
}

@CheckForNull
@TypeQualifierDefault(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MethodsAreCheckNullByDefault {

}
