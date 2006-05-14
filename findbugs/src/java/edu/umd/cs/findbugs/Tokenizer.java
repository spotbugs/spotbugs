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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.BitSet;

/**
 * A simple tokenizer for Java source text.
 * This is not intended to be a compliant lexer;
 * instead, it is for quick and dirty scanning.
 *
 * @author David Hovemeyer
 * @see Token
 */
public class Tokenizer {
	private static final BitSet whiteSpace = new BitSet();

	static {
		whiteSpace.set(' ');
		whiteSpace.set('\t');
		whiteSpace.set('\r');
		whiteSpace.set('\f');
	}

	private static final BitSet single = new BitSet();

	static {
		single.set('!');
		single.set('%');
		single.set('^');
		single.set('&');
		single.set('*');
		single.set('(');
		single.set(')');
		single.set('-');
		single.set('+');
		single.set('=');
		single.set('[');
		single.set(']');
		single.set('{');
		single.set('}');
		single.set('|');
		single.set(':');
		single.set(';');
		single.set(',');
		single.set('.');
		single.set('<');
		single.set('>');
		single.set('?');
		single.set('~');
	}

	private PushbackReader reader;

	/**
	 * Constructor.
	 *
	 * @param reader the Reader for the Java source text
	 */
	public Tokenizer(Reader reader) {
		this.reader = new PushbackReader(reader);
	}

	/**
	 * Get the next Token in the stream.
	 *
	 * @return the Token
	 */
	public Token next() throws IOException {
		skipWhitespace();
		int c = reader.read();
		if (c < 0)
			return new Token(Token.EOF);
		else if (c == '\n')
			return new Token(Token.EOL);
		else if (c == '\'' || c == '"')
			return munchString(c);
		else if (c == '/')
			return maybeComment();
		else if (single.get(c))
			return new Token(Token.SINGLE, String.valueOf((char) c));
		else {
			reader.unread(c);
			return parseWord();
		}
	}

	private void skipWhitespace() throws IOException {
		for (; ;) {
			int c = reader.read();
			if (c < 0) break;
			if (!whiteSpace.get(c)) {
				reader.unread(c);
				break;
			}
		}
	}

	private Token munchString(int delimiter) throws IOException {
		final int SCAN = 0;
		final int ESCAPE = 1;
		final int DONE = 2;

		StringBuffer result = new StringBuffer();
		result.append((char) delimiter);
		int state = SCAN;

			while (state != DONE) {
				int c = reader.read();
				if (c < 0)
					break;
				result.append((char) c);
				switch (state) {
				case SCAN:
					if (c == delimiter)
						state = DONE;
					else if (c == '\\')
						state = ESCAPE;
					break;
				case ESCAPE:
					state = SCAN;
					break;
				}
			}
		return new Token(Token.STRING, result.toString());
	}

	private Token maybeComment() throws IOException {
		int c = reader.read();
		if (c == '/') {
			// Single line comment
			StringBuffer result = new StringBuffer();
			result.append("//");
			for (; ;) {
				c = reader.read();
				if (c < 0)
					break;
				else if (c == '\n') {
					reader.unread(c);
					break;
				}
				result.append((char) c);
			}
			return new Token(Token.COMMENT, result.toString());
		} else if (c == '*') {
			// C-style multiline comment
			StringBuffer result = new StringBuffer();
			result.append("/*");
			final int SCAN = 0;
			final int STAR = 1;
			final int DONE = 2;
			int state = SCAN;
			while (state != DONE) {
				c = reader.read();
				if (c < 0)
					state = DONE;
				else
					result.append((char) c);
				switch (state) {
				case SCAN:
					if (c == '*')
						state = STAR;
					break;
				case STAR:
					if (c == '/')
						state = DONE;
					else if (c != '*')
						state = SCAN;
					break;
				case DONE:
					break;
				}
			}
			return new Token(Token.COMMENT, result.toString());
		} else {
			if (c >= 0)
				reader.unread(c);
			return new Token(Token.SINGLE, "/");
		}
	}

	private Token parseWord() throws IOException {
		StringBuffer result = new StringBuffer();
		for (; ;) {
			int c = reader.read();
			if (c < 0)
				break;
			if (whiteSpace.get(c) || c == '\n' || single.get(c)) {
				reader.unread(c);
				break;
			}
			result.append((char) c);
		}
		return new Token(Token.WORD, result.toString());
	}
}

// vim:ts=4
