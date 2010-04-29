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
 * Annotation indicating that a FindBugs warning is desired.
 * 
 * @author David Hovemeyer
 */
@Retention(RetentionPolicy.CLASS)
public @interface DesireWarning {
	// The value indicates the bug code (e.g., NP) of the desired warning.
	public String value();
	public boolean bugPattern() default false;
}
