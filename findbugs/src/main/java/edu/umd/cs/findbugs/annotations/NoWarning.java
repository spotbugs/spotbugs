/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that <em>no</em> FindBugs warning is expected.
 *
 * See http://code.google.com/p/findbugs/wiki/FindbugsTestCases
 *
 * @author David Hovemeyer
 * @deprecated The annotation based approach is useless for lambdas. Write expectations using {@code edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher} matchers in test source directory
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
public @interface NoWarning {
    /**
     * The value indicates the bug code (e.g., NP) or bug pattern (e.g.,
     * IL_INFINITE_LOOP) that should not be reported
     */
    public String value();

    /** Want no warning at this priority or higher */
    public Confidence confidence() default Confidence.LOW;

    /** Want no warning at this rank or scarier */
    public int rank() default 20; // BugRanker.VISIBLE_RANK_MAX

    /** Tolerate up to this many warnings */
    public int num() default 0;
}
