/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005 University of Maryland
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Was used to suppress FindBugs warnings but generates name conflicts with {@link java.lang.SuppressWarnings}.
 *
 *
 * @deprecated - Use {@link SuppressFBWarnings} instead
 * @author pugh
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR,
    ElementType.LOCAL_VARIABLE, ElementType.PACKAGE })
@Retention(RetentionPolicy.CLASS)
@Deprecated
public @interface SuppressWarnings {
    /**
     * The set of FindBugs warnings that are to be suppressed by the compiler in the
     * annotated element.
     *
     */
    String[] value() default {};

    String justification() default "";
}
