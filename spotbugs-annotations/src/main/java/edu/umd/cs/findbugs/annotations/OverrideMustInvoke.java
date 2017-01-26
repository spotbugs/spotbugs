/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005-2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a method that, if overridden, must (or should) be invoked by
 * an invocation on super in the overriding method. Examples of such methods
 * include finalize() and clone().
 *
 * The argument to the method indicates when the super invocation should occur:
 * at any time, at the beginning of the overriding method, or at the end of the
 * overriding method.
 *
 * @see edu.umd.cs.findbugs.annotations.When
 *
 * @deprecated - Use {@link javax.annotation.OverridingMethodsMustInvokeSuper} instead
 **/
@Documented
@Deprecated
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface OverrideMustInvoke {
    When value() default When.ANYTIME;

}
