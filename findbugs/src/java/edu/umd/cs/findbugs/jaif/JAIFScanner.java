/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.jaif;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical scanner for external annotation files.
 * 
 * @author David Hovemeyer
 * @see http://pag.csail.mit.edu/jsr308/annotation-file-utilities/
 */
public class JAIFScanner {
	
	private static final Pattern HORIZ_WHITESPACE = 
		Pattern.compile("^[ \\t\\r\\f]+");

	static class TokenPattern {
		private Pattern pattern;
		private JAIFTokenKind kind;
		
		public TokenPattern(String regex, JAIFTokenKind kind) {
			this.pattern = Pattern.compile("^" + regex);
			this.kind = kind;
		}

		public JAIFTokenKind getKind(String lexeme) {
			return kind;
		}

		public Pattern getPattern() {
			return pattern;
		}
	}

	private static final String ID_START = "[@A-Za-z_\\$]";
	private static final String ID_REST  = "[A-Za-z0-9_\\$]";
	
	private static final TokenPattern[] TOKEN_PATTERNS = {
		new TokenPattern(":", JAIFTokenKind.COLON),
		new TokenPattern("\\(", JAIFTokenKind.LPAREN),
		new TokenPattern("\\)", JAIFTokenKind.RPAREN),
		new TokenPattern(",", JAIFTokenKind.COMMA),
		new TokenPattern("=", JAIFTokenKind.EQUALS),
		new TokenPattern(ID_START + "(" + ID_REST + "*)", JAIFTokenKind.IDENTIFIER_OR_KEYWORD),
	};
	
	private BufferedReader reader;
	private JAIFToken next;
	private String lineBuf;
	private int lineNum;

	/**
	 * @param reader
	 */
	public JAIFScanner(Reader reader) {
		this.reader = new BufferedReader(reader);
	}

	public int getLineNumber() {
		return lineNum;
	}

	public JAIFToken nextToken() throws IOException, JAIFSyntaxException {
		if (next == null) {
			fetchToken();
		}
		JAIFToken result = next;
		next = null;
		return result;
	}
	
	public JAIFToken peekToken() throws IOException, JAIFSyntaxException {
		if (next == null) {
			fetchToken();
		}
		return next;
	}

	public boolean atEOF() throws IOException {
		fillLineBuf();
		return lineBuf == null;
	}

	private void fillLineBuf() throws IOException {
	    if (lineBuf == null) {
			lineBuf = reader.readLine();
		}
    }
	
	private void fetchToken() throws IOException, JAIFSyntaxException {
		assert next == null;

		fillLineBuf();
		if (lineBuf == null) {
			throw new JAIFSyntaxException(this, "Unexpected end of file");
		}
		
		Matcher wsMatch = HORIZ_WHITESPACE.matcher(lineBuf);
		if (wsMatch.find()) {
			lineBuf = lineBuf.substring(wsMatch.group().length());
		}
		
		if (lineBuf.equals("")) {
			// Reached end of line.
			next = new JAIFToken(JAIFTokenKind.NEWLINE, "\n");
			lineBuf = null;
			return;
		}
		
		// Try matching line buffer against all known patterns
		// until we fine one that matches.
		for (TokenPattern tokenPattern : TOKEN_PATTERNS) {
			Matcher m = tokenPattern.getPattern().matcher(lineBuf);
			if (m.find()) {
				String lexeme = m.group();
				lineBuf = lineBuf.substring(lexeme.length());
				next = new JAIFToken(tokenPattern.getKind(lexeme), lexeme);
				return;
			}
		}
		
		throw new JAIFSyntaxException(this, "Unrecognized token (trying to match text `" + lineBuf + "'");
	}
}
