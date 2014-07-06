/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.internalAnnotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

/**
 * * Denotes a class name or package name where the / character is used to
 * separate package/class name components.
 *
 * @author pugh
 */
@Documented
@TypeQualifier(applicableTo = CharSequence.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlashedClassName {

    public static final String NOT_AVAILABLE = "./.";

    When when() default When.ALWAYS;

    static class Checker implements TypeQualifierValidator<SlashedClassName> {
        final static String simpleName = "(\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart}|\\$)*)";

        final static String slashedClassName = simpleName + "(/" + simpleName + ")*";

        final static Pattern simplePattern = Pattern.compile(simpleName);

        final static Pattern pattern = Pattern.compile(slashedClassName);

        @Override
        @Nonnull
        public When forConstantValue(@Nonnull SlashedClassName annotation, Object value) {
            if (!(value instanceof String)) {
                return When.UNKNOWN;
            }

            if (pattern.matcher((String) value).matches()) {
                return When.ALWAYS;
            }

            if (value.equals(NOT_AVAILABLE)) {
                return When.MAYBE;
            }

            return When.NEVER;

        }

    }
}
