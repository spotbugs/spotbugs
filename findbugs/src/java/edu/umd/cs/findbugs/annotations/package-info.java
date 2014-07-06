/**
 * Annotations for FindBugs (mostly deprecated except for {@link edu.umd.cs.findbugs.annotations.SuppressFBWarnings}).
 *
 * This annotations are mostly deprecated and replaced by JSR 305 annotations
 * defined in javax.annotation. The annotations still actively supported are:
 * <ul>
 * <li> {@link edu.umd.cs.findbugs.annotations.SuppressFBWarnings} for suppressing FindBugs warnings
 * <li> Annotations about expected/unexpected warnings in FindBugs regression tests
 * <ul>
 * <li>  {@link edu.umd.cs.findbugs.annotations.ExpectWarning} Warnings expected to be generated
 *  <li>  {@link edu.umd.cs.findbugs.annotations.NoWarning} Warnings that should not  be generated
 *  <li>  {@link edu.umd.cs.findbugs.annotations.DesireWarning} Warnings we wish to generated
 *  <li>  {@link edu.umd.cs.findbugs.annotations.DesireNoWarning} Warnings we wish to not generate generated
 *  </ul></ul>
 *
 *  There are another set of annotations used by an experimental detector for unclosed resources:
 *  <ul>
 *  <li>{@link edu.umd.cs.findbugs.annotations.CleanupObligation}
 *  <li>{@link edu.umd.cs.findbugs.annotations.CreatesObligation}
 *  <li>{@link edu.umd.cs.findbugs.annotations.DischargesObligation}
 *  </ul>

 */
package edu.umd.cs.findbugs.annotations;

