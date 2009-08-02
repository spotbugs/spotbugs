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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForFields;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForMethods;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.NonNull;

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
@DefaultAnnotationForFields(NonNull.class)
@DefaultAnnotationForMethods(CheckForNull.class)
@DefaultAnnotationForParameters(NonNull.class)

class Bug2672946B extends Bug2672946A {
    public Bug2672946B(Bug2672946B field){
        super(field);
    }

    @NoWarning("NP")
    @Override
    public Bug2672946B getField() {
        return (Bug2672946B) super.getField();
    }
}
