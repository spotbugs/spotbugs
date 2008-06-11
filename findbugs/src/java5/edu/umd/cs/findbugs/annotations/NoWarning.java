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
 * Annotation indicating that <em>no</em> FindBugs warning is expected.
 * 
 * @author David Hovemeyer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoWarning {
	// Comma-separated list of bug codes (e.g., "NP") not expected
	// in the annotated method.
	public String value();
}
