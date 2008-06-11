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
 * Annotation indicating that a FindBugs warning is expected.
 * 
 * @author David Hovemeyer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ExpectWarning {
	// The value indicates the bug code (e.g., NP) of the expected warning.
	public String value();
}
