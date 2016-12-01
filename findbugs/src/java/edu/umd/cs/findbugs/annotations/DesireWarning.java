/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that a FindBugs warning is desired.
 *
 * See http://code.google.com/p/findbugs/wiki/FindbugsTestCases
 *
 * @author David Hovemeyer
 * @deprecated The annotation based approach is useless for lambdas. Write expectations using {@code edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher} matchers in test source directory
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
public @interface DesireWarning {
    /**
     * The value indicates the bug code (e.g., NP) or bug pattern (e.g.,
     * IL_INFINITE_LOOP) of the desired warning
     */
    public String value();

    /** Want a warning at this priority or higher */
    public Confidence confidence() default Confidence.LOW;

    /** Desire a warning at least this scary */
    public int rank() default 20; // BugRanker.VISIBLE_RANK_MAX

    /** Desire at least this many warnings */
    public int num() default 1;
}
