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

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * @author rak
 */
public class SignatureUtil {

    public static String createMethodSignature(String params, String returns) {
        if (params == null && returns == null) {
            return null;
        }

        String pString, rString;
        if (params == null) {
            pString = ".*";
        } else {
            StringBuilder buf = new StringBuilder();

            StringTokenizer tok = new StringTokenizer(params, " \t\n\r\f,");
            while (tok.hasMoreTokens()) {
                String param = typeToSignature(tok.nextToken());
                buf.append(param);
            }
            pString = buf.toString();
        }
        if (returns == null) {
            rString = ".*";
        } else {
            rString = typeToSignature(returns);
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
        return typeToSignature(type);
    }

    private static String typeToSignature(String type) {
        if (type.endsWith("[]")) {
            return "[" + typeToSignature(type.substring(0, type.length() - 2));
        } else {
            return scalarTypeToSiganture(type);
        }
    }

    private static String scalarTypeToSiganture(String type) {
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
            return "L" + type.replace('.', '/') + ";";
        }
    }
}
