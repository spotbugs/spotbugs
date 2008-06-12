/*
 * Machine Learning support for FindBugs
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
package edu.umd.cs.findbugs.ml;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Split a camel case identifier into individual words.
 * 
 * @author David Hovemeyer
 */
public class SplitCamelCaseIdentifier {
	private String ident;

	/**
	 * Constructor.
	 * 
	 * @param ident the identifier to split into words
	 */
	public SplitCamelCaseIdentifier(String ident) {
		this.ident = ident;
	}

	/**
	 * Split the identifier into words.
	 * 
	 * @return Collection of words in the identifier
	 */
	public Collection<String> split() {
		String s = ident;
		Set<String> result = new HashSet<String>();

		while (s.length() > 0) {
			StringBuilder buf = new StringBuilder();

			char first = s.charAt(0);
			buf.append(first);
			int i = 1;

			if (s.length() > 1) {
				boolean camelWord;
				if (Character.isLowerCase(first)) {
					camelWord = true;
				} else {
					char next = s.charAt(i++);
					buf.append(next);
					camelWord = Character.isLowerCase(next);
				}

				while (i < s.length()) {
					char c = s.charAt(i);
					if (Character.isUpperCase(c)) {
						if (camelWord)
							break;
					} else if (!camelWord) {
						break;
					}
					buf.append(c);
					++i;
				}

				if (!camelWord && i < s.length()) {
					buf.deleteCharAt(buf.length() - 1);
					--i;
				}
			}

			result.add(buf.toString().toLowerCase(Locale.US));
			s = s.substring(i);
		}

		return result;
	}
}
