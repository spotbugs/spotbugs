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

/**
 * @author rak
 */
public class SignatureUtil {

	public static String createMethodSignature(String params, String returns) {
		StringBuffer buf = new StringBuffer();

		buf.append('(');
		StringTokenizer tok = new StringTokenizer(params, " \t\n\r\f,");
		while (tok.hasMoreTokens()) {
			String param = typeToSignature(tok.nextToken());
			buf.append(param);
		}
		buf.append(')');
		buf.append(typeToSignature(returns));

		return buf.toString();
	}

	public static String createFieldSignature(String type) {
		return typeToSignature(type);
	}

	private static String typeToSignature(String type) {
		if(type.endsWith("[]")) {
			return "[" + typeToSignature(type.substring(0, type.length() - 2));
		} else {
			return scalarTypeToSiganture(type);
		}
	}

	private static String scalarTypeToSiganture(String type) {
		if (type.equals("boolean"))
			return "Z";
		else if (type.equals("byte"))
			return "B";
		else if (type.equals("char"))
			return "C";
		else if (type.equals("short"))
			return "S";
		else if (type.equals("int"))
			return "I";
		else if (type.equals("long"))
			return "J";
		else if (type.equals("float"))
			return "F";
		else if (type.equals("double"))
			return "D";
		else if (type.equals("void"))
			return "V";
		else
			return "L" + type.replace('.', '/') + ";";
	}
}
