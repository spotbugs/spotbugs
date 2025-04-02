/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.filter;

import edu.umd.cs.findbugs.util.ClassName;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * @author rak
 */
public class SignatureUtil {

    /**
     * @param params The parameters for this method signature, or null, for instance <code>int, long</code>
     * @param returns The return for this method signature, or null, for instance <code>double</code>
     * @return The method signature or (in case either <code>params</code> or <code>returns</code> are null) a regex pattern matching the signatures. When a regex is returned the first character will be '~'.
     */
    public static String createMethodSignature(String params, String returns) {
        if (params == null && returns == null) {
            return null;
        }

        String pString;
        String rString;
        if (params == null) {
            pString = ".*";
        } else {
            StringBuilder buf = new StringBuilder();

            StringTokenizer tok = new StringTokenizer(params, " \t\n\r\f,");
            while (tok.hasMoreTokens()) {
                String param = typeToSignature(tok.nextToken(), returns == null);
                buf.append(param);
            }
            pString = buf.toString();
        }
        if (returns == null) {
            rString = ".*";
        } else {
            rString = typeToSignature(returns, params == null);
        }
        if (params == null || returns == null) {
            String result = "~\\(" + pString + "\\)" + rString;
            assert Pattern.compile(result.substring(1)) != null;
            return result;
        } else {
            return "(" + pString + ")" + rString;
        }

    }

    public static String createFieldSignature(String type) {
        if (type == null) {
            return null;
        }
        return typeToSignature(type, false);
    }

    private static String typeToSignature(String type, boolean regex) {
        if (type.endsWith("[]")) {
            String typeString = "[" + typeToSignature(type.substring(0, type.length() - 2), regex);
            if (regex) {
                // When we're building a regex we need to escape the '[' or it will be processed as a regex group
                return "\\" + typeString;
            } else {
                return typeString;
            }
        } else {
            return scalarTypeToSignature(type);
        }
    }

    private static String scalarTypeToSignature(String type) {
        if ("boolean".equals(type)) {
            return "Z";
        } else if ("byte".equals(type)) {
            return "B";
        } else if ("char".equals(type)) {
            return "C";
        } else if ("short".equals(type)) {
            return "S";
        } else if ("int".equals(type)) {
            return "I";
        } else if ("long".equals(type)) {
            return "J";
        } else if ("float".equals(type)) {
            return "F";
        } else if ("double".equals(type)) {
            return "D";
        } else if ("void".equals(type)) {
            return "V";
        } else {
            return "L" + ClassName.toSlashedClassName(type) + ";";
        }
    }
}
