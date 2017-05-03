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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jsr305.FieldsNonNullByDefault;

/* ********************
 * Behavior at filing: False positive NP, see Bug2672946B.java for details
 * ********************
 */
@ParametersAreNonnullByDefault
@FieldsNonNullByDefault
@MethodsAreCheckNullByDefault
class Bug2672946A {

    private Bug2672946A field;

    public Bug2672946A(Bug2672946A field) {
        this.field = field;
    }

    @Nonnull
    public Bug2672946A getField() {
        return this.field;
    }
    
    @Nonnull
    public Bug2672946A getField2() {
        return this.field;
    }
}
