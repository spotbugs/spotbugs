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

package edu.umd.cs.findbugs.ba;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author pugh
 */
public class NullnessAnnotation extends AnnotationEnumeration<NullnessAnnotation> {
    public final static NullnessAnnotation CHECK_FOR_NULL = new NullnessAnnotation("CheckForNull", 3);

    public final static NullnessAnnotation NONNULL = new NullnessAnnotation("NonNull", 1);

    public final static NullnessAnnotation NULLABLE = new NullnessAnnotation("Nullable", 2);

    public final static NullnessAnnotation UNKNOWN_NULLNESS = new NullnessAnnotation("UnknownNullness", 0);

    private final static NullnessAnnotation[] myValues = { UNKNOWN_NULLNESS, NONNULL, NULLABLE, CHECK_FOR_NULL };

    public static class Parser {
        @CheckForNull
        public static NullnessAnnotation parse(@DottedClassName String className) {
            className = ClassName.toDottedClassName(className);
            if ("com.google.common.base.Nullable".equals(className)
                    || "org.eclipse.jdt.annotation.Nullable".equals(className)
                    || "org.jetbrains.annotations.Nullable".equals(className)) {
                return CHECK_FOR_NULL;
            }
            // Unfortunately there are mixed case Nonnull and NonNull annotations (JSR305, FB and JDT)
            if (className.endsWith("Nonnull")) {
                return NONNULL;
            }
            for (NullnessAnnotation v : myValues) {
                if (className.endsWith(v.name)) {
                    return v;
                }
            }
            if (className.endsWith("PossiblyNull")) {
                return CHECK_FOR_NULL;
            }
            return null;
        }
    }

    public static NullnessAnnotation[] values() {
        return myValues.clone();
    }

    private NullnessAnnotation(String s, int i) {
        super(s, i);
    }

}
