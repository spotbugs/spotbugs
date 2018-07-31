/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * a class holding common constants used throughout FindBugs
 */
public final class Values {

    public static final String SIG_GENERIC_TEMPLATE = "T";
    public static final String SIG_QUALIFIED_CLASS_PREFIX = "L";
    public static final String SIG_QUALIFIED_CLASS_SUFFIX = ";";
    public static final char SIG_QUALIFIED_CLASS_SUFFIX_CHAR = ';';
    public static final String SIG_ARRAY_PREFIX = "[";

    @DottedClassName
    public static final String DOTTED_JAVA_LANG_OBJECT = "java.lang.Object";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_STRING = "java.lang.String";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_CLASS = "java.lang.Class";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_THROWABLE = "java.lang.Throwable";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_EXCEPTION = "java.lang.Exception";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_RUNTIMEEXCEPTION = "java.lang.RuntimeException";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_ERROR = "java.lang.Error";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_INTEGER = "java.lang.Integer";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_STRINGBUILDER = "java.lang.StringBuilder";
    @DottedClassName
    public static final String DOTTED_JAVA_LANG_STRINGBUFFER = "java.lang.StringBuffer";
    @DottedClassName
    public static final String DOTTED_JAVA_IO_FILE = "java.io.File";
    @DottedClassName
    public static final String DOTTED_JAVA_NIO_PATH = "java.nio.file.Path";

    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_OBJECT = "java/lang/Object";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_STRING = "java/lang/String";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_STRINGBUILDER = "java/lang/StringBuilder";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_STRINGBUFFER = "java/lang/StringBuffer";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_CLASS = "java/lang/Class";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_THROWABLE = "java/lang/Throwable";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_EXCEPTION = "java/lang/Exception";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_RUNTIMEEXCEPTION = "java/lang/RuntimeException";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_ERROR = "java/lang/Error";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_INTEGER = "java/lang/Integer";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_LONG = "java/lang/Long";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_FLOAT = "java/lang/Float";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_DOUBLE = "java/lang/Double";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_SHORT = "java/lang/Short";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_BYTE = "java/lang/Byte";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_CHARACTER = "java/lang/Character";
    @SlashedClassName
    public static final String SLASHED_JAVA_LANG_BOOLEAN = "java/lang/Boolean";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_COMPARATOR = "java/util/Comparator";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_COLLECTION = "java/util/Collection";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_LIST = "java/util/List";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_SET = "java/util/Set";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_MAP = "java/util/Map";
    @SlashedClassName
    public static final String SLASHED_JAVA_UTIL_UUID = "java/util/UUID";

    private Values() {
    }
}
