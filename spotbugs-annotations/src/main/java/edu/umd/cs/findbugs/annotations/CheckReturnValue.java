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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to denote a method whose return value should always
 * be checked when invoking the method.
 *
 * The checker treats this annotation as inherited by overriding methods.
 *
 * @deprecated - use {@link javax.annotation.CheckReturnValue} instead.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.CLASS)
@Deprecated
public @interface CheckReturnValue {

    @Deprecated
    Priority priority() default Priority.MEDIUM;

    Confidence confidence() default Confidence.MEDIUM;

    /**
     * A textual explanation of why the return value should be checked
     */
    String explanation() default "";
}
