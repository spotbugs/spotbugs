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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.util;

/** A class for static String utility methods.
 * @author Brian Cole
 */
public class Strings {

	/** This is intended to be semantically equivalent to <code>source.replace(find, repl)</code>
	 *  but also compatible with JDK 1.4.
	 * @param source The String on which to operate
	 * @param find   The literal substring to be replaced
	 * @param repl   The literal replacement substring
	 * @return       The resultant String after substitution
	 * @throws NullPointerException if any of the arguments are null
	 * @throws IllegalArgumentException if <code>find</code> has zero length
	 * @see java.lang.String#replace(CharSequence target, CharSequence replacement)
	 */
	public static String replace(String source, String find, String repl) {
		/* JDK 1.5 uses a regex (with Pattern.LITERAL) to implement this. We could do
		   that too, but why don't we just use StringBuffer (not 1.5's StringBuilder) */
		int j = source.indexOf(find); // -1 if not found
		if (j < 0) return source; // nothing to replace
		final int findLen = find.length();
		if (findLen <= 0) throw new IllegalArgumentException("unable to replace all occurrences of the empty String");
		int anchor = 0;
		StringBuffer sb = new StringBuffer(source.length()+repl.length()); // guess final length
		while (j >= 0) {
			sb.append(source.substring(anchor, j));
			sb.append(repl);
			anchor = j+findLen;
			j = source.indexOf(find, anchor);
		}
		sb.append(source.substring(anchor));
		return sb.toString();
	}

	/** This is intended to be equivalent to <code>Arrays.toString(a)</code>
	 *  but also compatible with JDK 1.4.
	 *  This concatenates the results of calling String.valueOf() on each element
	 *  of the array, so this won't work well for multi-dimensional arrays.
	 * @see java.lang.String#valueOf(Object)
	 * @see java.util.Arrays#toString(Object[])
	 * @see java.util.Arrays#deepToString(Object[])
	 */
	public static String toString(final Object[] a) {
		if (a == null) return "null";
		int max = a.length - 1;
		StringBuffer sb = new StringBuffer("[");
		for (int j=0; j <= max; j+=1) {
			sb.append(String.valueOf(a[j]));
			if (j < max) sb.append(','); // Arrays.toString() appends ", "
		}
		sb.append(']');
		return sb.toString();
	}

}
