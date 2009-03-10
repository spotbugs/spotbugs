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
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotationForFields(NonNull.class)
@DefaultAnnotationForMethods(CheckForNull.class)
@DefaultAnnotationForParameters(NonNull.class)

/* ********************
 * Behavior at filing:  False positive NP, see Bug2672946B.java for details
 * ******************** */

class Bug2672946A {

    private Bug2672946A field;

    public Bug2672946A(Bug2672946A field){
        this.field = field;
    }

    @NonNull
    public Bug2672946A getField(){
        return this.field;
    }
}
