/**
 * Annotations for FindBugs. Mostly deprecated and replaced by JSR 305 annotations
 * defined in {@link javax.annotation}.
 * 
 * The annotations still actively supported are:
 * <ul>
 * <li> {@link SuppressFBWarnings} for suppressing FindBugs warnings
 * <li> Annotations about expected/unexpected warnings in FindBugs regression tests
 * <ul>
 * <li>  {@link ExpectWarning} Warnings expected to be generated
 *  <li>  {@link NoWarning} Warnings that should not  be generated
 *  <li>  {@link DesireWarning} Warnings we wish to generated
 *  <li>  {@link DesireNoWarning} Warnings we wish to not generate generated
 *  </ul></ul>
 */
package edu.umd.cs.findbugs.annotations;

