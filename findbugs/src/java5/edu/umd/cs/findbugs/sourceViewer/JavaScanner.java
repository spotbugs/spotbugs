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

package edu.umd.cs.findbugs.sourceViewer;

import java.text.CharacterIterator;
import java.util.HashSet;

public class JavaScanner {
	public final static int NORMAL_TEXT = 0;
	public final static int COMMENT= 1;
	public final static int JAVADOC = 2;

	public final static int KEYWORD = 3;
	public final static int QUOTE= 4;
	public final static int EOF = -1;




	private final static HashSet<String> KEYWORDS = new HashSet<String>();

	private final static int MAX_KEYWORD_LENGTH;





	static {
		String[] keywordList = new String[] { "abstract", "assert", "boolean",
				"break", "byte", "case", "catch", "char", "class", "const",
				"continue", "default", "do", "double", "else", "enum",
				"extends", "false", "final", "finally", "float", "for", "goto",
				"if", "implements", "import", "instanceof", "int", "interface",
				"long", "native", "new", "null", "package", "private",
				"protected", "public", "return", "short", "static", "strictfp",
				"super", "switch", "synchronized", "this", "throw", "throws",
				"transient", "true", "try", "void", "volatile", "while" };
		int max = 0;
		for (String s : keywordList) {
			if (max < s.length())
				max = s.length();
			KEYWORDS.add(s);
		}
		MAX_KEYWORD_LENGTH = max;
	}

	private final StringBuffer buf = new StringBuffer();

	private int endPosition;

	private final CharacterIterator iterator;

	private int kind;

	private int startPosition;

	public JavaScanner(CharacterIterator iterator) {
		this.iterator = iterator;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return iterator.getIndex();
	}

	public int getLength() {
		return  iterator.getIndex() - startPosition;
	}
	public int getKind() {
		return kind;
	}
	public int next() {
		startPosition = iterator.getIndex();
		char c = iterator.current();
		iterator.next(); // advance
		if (c == CharacterIterator.DONE) {
			kind = EOF;
		}
		else if (Character.isJavaIdentifierStart(c)) {
			buf.append(c);
			boolean couldBeKeyword = Character.isLowerCase(c);
			while (true) {
				c = iterator.current();
				if (!Character.isJavaIdentifierPart(c))
					break;
				buf.append(c);
				if (couldBeKeyword) {
					if (!Character.isLowerCase(c)
							|| buf.length() >= MAX_KEYWORD_LENGTH)
						couldBeKeyword = false;
				}
				c = iterator.next();
			}
			kind = NORMAL_TEXT;
			if (couldBeKeyword) {
				if (KEYWORDS.contains(buf.toString()))
					kind = KEYWORD;
			}
			buf.setLength(0);
		} else if (c == '/') {
			char c2 = iterator.current();
			if (c2 == '/') {
				while (true) {
					c2 = iterator.next();
					if (c2 == '\n' || c2 == '\r' || c2 == CharacterIterator.DONE)
						break;
				}
				kind = COMMENT;
				return kind;
			} else if (c2 == '*') {
				scanComment: while (c2 != CharacterIterator.DONE) {
					c2 = iterator.next();
					if (c2 == '*') {
						do {
						c2 = iterator.next();
						if (c2 == '/')
							break scanComment;
						} while(c2 == '*');
					}
				}
				kind = JAVADOC;
				return kind;
			}
		} else if (c == '"') {
			kind = QUOTE;
			char c2 = iterator.current();
			while (c2 != '"' && c2 != '\n' && c2 != '\r' && c2 != CharacterIterator.DONE) {
				if (c2 == '\\') {
					c2 = iterator.next();
					if (c2 == '\n' || c2 == '\r')
						break;
				}
				c2 = iterator.next();
			}
			iterator.next(); // advance past closing char
		} else if (c == '\'') {
			 // need to catch '"' so isn't considered to start a String
			kind = QUOTE; // or NORMAL_TEXT ?
			char c2 = iterator.current();
			if (c2 == '\\') c2 = iterator.next(); // advance past the escape char
			if (c2 != '\n' && c2 != '\r' && c2 != CharacterIterator.DONE)
				c2 = iterator.next(); // advance past the content char
			if (c2 != '\n' && c2 != '\r' && c2 != CharacterIterator.DONE)
				iterator.next(); // advance past closing char

		} else
			kind = NORMAL_TEXT;
		// System.out.println(kind + " " + startPosition + "-" + iterator.getIndex());
		return kind;
	}

}
