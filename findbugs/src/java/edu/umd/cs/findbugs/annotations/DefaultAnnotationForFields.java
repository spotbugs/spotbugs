/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that all members of the class or package should be
 * annotated with the default value of the supplied annotation class.
 * 
 * This would be used for behavior annotations such as @NonNull, @CheckForNull,
 * or @CheckReturnValue.
 * 
 * In particular, you can use @DefaultAnnotation(NonNull.class) on a class or package,
 * and then use @Nullable only on those parameters, methods or fields that you want
 * to allow to be null.
 *
 * @author William Pugh
 */

@Documented
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)

public @interface DefaultAnnotationForFields {
	Class<? extends Annotation>[] value();
    Priority priority() default Priority.MEDIUM;
}

