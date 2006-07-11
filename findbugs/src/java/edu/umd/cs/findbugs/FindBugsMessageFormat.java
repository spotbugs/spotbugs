/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

/**
 * Format the message for a BugInstance.
 * This class works in much the same way as <code>java.text.MessageFormat</code>;
 * however, each placeholder may have an optional "key" which specifies
 * how the object at that position should be formatted.
 * <p/>
 * <p> Example:
 * <pre>
 *     new FindBugsMessageFormat("BUG: {1} does something bad to field {2.fullField}")
 * </pre>
 * In this example, the method annotation at position 1 is formatted using
 * the empty (default) key.  The field annotation at position 2 is formatted
 * using the "fullField" key, which uses the long format for the field rather
 * than the usual "class.fieldname" format.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public class FindBugsMessageFormat {
	private String pattern;

	/**
	 * Constructor.
	 *
	 * @param pattern the pattern for the message
	 */
	public FindBugsMessageFormat(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Format the message using the given array of BugAnnotations as arguments
	 * to bind to the placeholders in the pattern string.
	 *
	 * @param args the BugAnnotations used as arguments
	 * @return the formatted message
	 */
	public String format(BugAnnotation[] args) {
		String pat = pattern;
		StringBuffer result = new StringBuffer();

		while (pat.length() > 0) {
			int subst = pat.indexOf('{');
			if (subst < 0) {
				result.append(pat);
				break;
			}

			result.append(pat.substring(0, subst));
			pat = pat.substring(subst + 1);

			int end = pat.indexOf('}');
			if (end < 0)
				throw new IllegalStateException("bad pattern " + pattern);

			String substPat = pat.substring(0, end);

			int dot = substPat.indexOf('.');
			String key = "";
			if (dot >= 0) {
				key = substPat.substring(dot + 1);
				substPat = substPat.substring(0, dot);
			}

			int fieldNum;
			try {
				fieldNum = Integer.parseInt(substPat);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("bad pattern " + pattern);
			}

			// System.out.println("fn: " + fieldNum);
			if (fieldNum < 0) {
				result.append("?<?" + fieldNum +  "/" + args.length + "???");
			} else if (fieldNum > args.length) {
					result.append("?>?" + fieldNum +  "/" + args.length + "???");
			} else {
				BugAnnotation field = args[fieldNum];
				result.append(field.format(key));
			}

			pat = pat.substring(end + 1);
		}

		return result.toString();
	}
}

// vim:ts=4
