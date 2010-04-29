/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that <em>no</em> FindBugs warning of the specified type
 * is desired.
 * 
 * @author David Hovemeyer
 */
@Retention(RetentionPolicy.CLASS)
public @interface DesireNoWarning {
	// Comma-separated list of bug codes (e.g., "NP") not desired
	// in the annotated method.
	public String value();
	public boolean bugPattern() default false;
}
