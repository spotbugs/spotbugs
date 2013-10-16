/**
 * Annotations for FindBugs (mostly deprecated except for {@link SuppressFBWarnings}).
 * 
 * This annotations are mostly deprecated and replaced by JSR 305 annotations
 * defined in {@link javax.annotation}. The annotations still actively supported are:
 * <ul>
 * <li> {@link SuppressFBWarnings} for suppressing FindBugs warnings
 * <li> Annotations about expected/unexpected warnings in FindBugs regression tests
 * <ul>
 * <li>  {@link ExpectWarning} Warnings expected to be generated
 *  <li>  {@link NoWarning} Warnings that should not  be generated
 *  <li>  {@link DesireWarning} Warnings we wish to generated
 *  <li>  {@link DesireNoWarning} Warnings we wish to not generate generated
 *  </ul></ul>
 * 
 *  There are another set of annotations used by an experimental detector for unclosed resources:
 *  <ul>
 *  <li>{@link CleanupObligation}
 *  <li>{@link CreatesObligation}
 *  <li>{@link DischargesObligation}
 *  </ul>

 */
package edu.umd.cs.findbugs.annotations;

