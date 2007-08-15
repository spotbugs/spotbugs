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

	// See http://java.sun.com/docs/books/jls/third_edition/html/lexical.html
	// Hexidecimal floating-point literals are not implemented.

	private static final String ID_START = "[@A-Za-z_\\$]";
	private static final String ID_REST  = "[A-Za-z0-9_\\$]";
	private static final String DIGIT = "[0-9]";
	private static final String DIGITS = DIGIT + "+";
	private static final String DIGITS_OPT = DIGIT + "*";
	private static final String SIGN_OPT = "[+-]?";
	private static final String DOT = "\\.";
	private static final String EXP_PART = "([Ee]" + SIGN_OPT + DIGITS + ")";
	private static final String EXP_PART_OPT = EXP_PART + "?";
	private static final String FLOAT_TYPE_SUFFIX = "[FfDd]";
	private static final String FLOAT_TYPE_SUFFIX_OPT = FLOAT_TYPE_SUFFIX + "?";
	private static final String OCTAL_DIGITS = "[0-7]+";
	private static final String HEX_SIGNIFIER = "0[Xx]";
	private static final String HEX_DIGITS = "[0-9A-Fa-f]+";
	private static final String INT_TYPE_SUFFIX_OPT = "[Ll]?";
	
	private static final TokenPattern[] TOKEN_PATTERNS = {
		// Misc. syntax
		new TokenPattern(":", JAIFTokenKind.COLON),
		new TokenPattern("\\(", JAIFTokenKind.LPAREN),
		new TokenPattern("\\)", JAIFTokenKind.RPAREN),
		new TokenPattern(",", JAIFTokenKind.COMMA),
		new TokenPattern("=", JAIFTokenKind.EQUALS),
		
		// Identifiers and keywords
		new TokenPattern(ID_START + "(" + ID_REST + ")*", JAIFTokenKind.IDENTIFIER_OR_KEYWORD),
		
		// FP literals
		new TokenPattern(DIGITS + DOT + DIGITS_OPT + EXP_PART_OPT + FLOAT_TYPE_SUFFIX_OPT, JAIFTokenKind.FLOATING_POINT_LITERAL),
		new TokenPattern(DOT + DIGITS + EXP_PART_OPT + FLOAT_TYPE_SUFFIX_OPT, JAIFTokenKind.FLOATING_POINT_LITERAL),
		new TokenPattern(DIGITS + EXP_PART + FLOAT_TYPE_SUFFIX_OPT, JAIFTokenKind.FLOATING_POINT_LITERAL),
		new TokenPattern(DIGITS + EXP_PART_OPT + FLOAT_TYPE_SUFFIX, JAIFTokenKind.FLOATING_POINT_LITERAL),
		
		// This must come after the FP literal patterns
		new TokenPattern(DOT, JAIFTokenKind.DOT),
		
		// Integer literals
		new TokenPattern("0" + OCTAL_DIGITS + INT_TYPE_SUFFIX_OPT, JAIFTokenKind.OCTAL_LITERAL),
		new TokenPattern(HEX_SIGNIFIER + HEX_DIGITS + INT_TYPE_SUFFIX_OPT, JAIFTokenKind.HEX_LITERAL),
		new TokenPattern(DIGITS + INT_TYPE_SUFFIX_OPT, JAIFTokenKind.DECIMAL_LITERAL),
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
		this.lineNum = 0;
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
			if (lineBuf != null) {
				++lineNum;
			}
		}
    }
	
	private boolean isHorizWhitespace(char c) {
		return c == ' ' || c == '\t';
	}
	
	private void fetchToken() throws IOException, JAIFSyntaxException {
		assert next == null;

		fillLineBuf();
		if (lineBuf == null) {
			throw new JAIFSyntaxException(this, "Unexpected end of file");
		}

		// Strip leading whitespace, if any
		int wsCount = 0;
		while (wsCount < lineBuf.length() && isHorizWhitespace(lineBuf.charAt(wsCount))) {
			wsCount++;
		}
		if (wsCount > 0) {
			lineBuf = lineBuf.substring(wsCount);
		}
		//System.out.println("Consumed " + wsCount + " characters of horizontal whitespace");
		
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
