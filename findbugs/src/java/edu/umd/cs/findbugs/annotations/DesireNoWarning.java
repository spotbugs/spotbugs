/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that <em>no</em> FindBugs warning of the specified type
 * is not desired.
 *
 * See http://code.google.com/p/findbugs/wiki/FindbugsTestCases
 *
 * @author David Hovemeyer
 */
@Retention(RetentionPolicy.CLASS)
public @interface DesireNoWarning {
    /**
     * The value indicates the bug code (e.g., NP) or bug pattern (e.g.,
     * IL_INFINITE_LOOP) that is desired to not be reported
     */
    public String value();

    @Deprecated
    Priority priority() default Priority.LOW;
    
    /** Want no warning at this priority or higher */
    Confidence confidence() default Confidence.LOW;
}
