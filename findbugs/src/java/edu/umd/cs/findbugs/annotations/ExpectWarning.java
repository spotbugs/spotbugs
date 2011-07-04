/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that a FindBugs warning is expected.
 *
 * See http://code.google.com/p/findbugs/wiki/FindbugsTestCases
 *
 * @author David Hovemeyer
 */
@Retention(RetentionPolicy.CLASS)
public @interface ExpectWarning {
    /**
     * The value indicates the bug code (e.g., NP) or bug pattern (e.g.,
     * IL_INFINITE_LOOP) of the expected warning. Can be a comma-separated list.
     */
    public String value();

    /** Want a warning at this priority or higher */
    public Priority priority() default Priority.LOW;

    public int num() default 1;
}
