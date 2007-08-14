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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * Parse an external annotation file.
 * 
 * @author David Hovemeyer
 * @see http://pag.csail.mit.edu/jsr308/annotation-file-utilities/
 */
public class JAIFParser {
	enum TokenKind {
		NEWLINE,
		COLON,
		DOT,
		IDENTIFIER;
	}
	
	static class Token {
		TokenKind kind;
		String lexeme;
		
		public Token(TokenKind kind, String lexeme) {
			this.kind = kind;
			this.lexeme = lexeme;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			if (lexeme.equals("\n")) {
				return "<newline>";
			} else {
				return lexeme;
			}
		}
	}
	
	private PushbackReader reader;
	private JAIFEvents callback;
	private int lineNumber;
	private Token next;
	
	public JAIFParser(Reader reader, JAIFEvents callback) {
		this.reader = new PushbackReader(reader, 16);
		this.callback = callback;
		this.lineNumber = 1;
	}
	
	public void parse() throws IOException, JAIFSyntaxException {
		parseAnnotationFile();
	}

	int getLineNumber() {
		return lineNumber;
	}

	// Parsing notes:
	// - Because the '\n' character is significant,
	//   we don't consider it to be a space character.
	
	private int read() throws IOException {
		int c = reader.read();
		if (c == '\n') {
			lineNumber++;
		}
		return c;
	}
	
	private void unread(int c) throws IOException {
		reader.unread(c);
	}
	
	private boolean atEOF() throws IOException {
		skipWhitespaceAndComments();
		int c = read();
		if (c < 0) {
			return true;
		}
		unread(c);
		return false;
	}
	
	private void skipWhitespaceAndComments() throws IOException {
		while (true) {
			skipWhitespace();
			int c = read();
			if (c < 0) {
				return;
			}
			
			// See if we're at the beginning of a comment
			if (c != '/') {
				unread(c);
				return;
			}
			
			// We have read exactly one forward slash. Try next character.
			c = read();
			
			if (c < 0) {
				// Reached EOF - put back the forward slash
				unread('/');
				return;
			}
			
			if (c != '/') {
				// The first forward slash was followed by some other character.
				// Rewind and return.
				unread(c);
				unread('/');
				return;
			}
			
			// Read two forward slashes - skip all input to end of line
			skipToEOL();
		}
	}
	
	private void skipWhitespace() throws IOException {
		int c;
		while ((c = read()) >= 0 && Character.isSpaceChar(c) && c != '\n') {
			// ignore
		}
		if (c >= 0) {
			unread(c);
		}
	}
	
	private void skipToEOL() throws IOException {
		int c;
		while ((c = read()) >= 0 && c != '\n') {
			// ignore
		}
	}
	
	private void fetchToken() throws IOException, JAIFSyntaxException {
		assert next == null;
		
		skipWhitespaceAndComments();
		if (atEOF()) {
			throw new JAIFSyntaxException(this, "Unexpected end of file");
		}
		int c = read();
		
		if (c == '\n') {
			next = new Token(TokenKind.NEWLINE, "\n");
		} else if (c == ':') {
			next = new Token(TokenKind.COLON, ":");
		} else if (c == '.') {
			next = new Token(TokenKind.DOT, ".");
		} else if (c == '@' || Character.isJavaIdentifierStart(c)) {
			next = readIdentifier(c);
		} else {
			throw new JAIFSyntaxException(this, "Unexpected character " + ((char) c));
		}
	}
	
	private Token peekToken() throws IOException, JAIFSyntaxException {
		if (next == null) {
			fetchToken();
		}
		return next;
	}
	
	private Token readToken() throws IOException, JAIFSyntaxException {
		if (next == null) {
			fetchToken();
		}
		Token result = next;
		next = null;
		return result;
	}
	
	private Token readIdentifier(int start) throws IOException {
		StringBuffer buf = new StringBuffer();
		buf.append((char) start);
		
		int c;
		while ((c = read()) >= 0) {
			if (!Character.isJavaIdentifierPart(c)) {
				unread(c);
				break;
			}
			buf.append((char) c);
		}
		
		return new Token(TokenKind.IDENTIFIER, buf.toString());
	}
	
	private void expect(String s) throws IOException, JAIFSyntaxException {
		Token t = readToken();
		if (!t.lexeme.equals(s)) {
			throw new JAIFSyntaxException(this, "Unexpected token " + t + " (was expecting " + s + ")");
		}
	}

	private String readCompoundName() throws IOException, JAIFSyntaxException {
		StringBuffer buf = new StringBuffer();
		
		while (true) {
			Token t = readToken();
			assert t.kind == TokenKind.IDENTIFIER;
			buf.append(t.lexeme);
			t = peekToken();
			if (t.kind != TokenKind.DOT) {
				break;
			} else {
				buf.append(t.lexeme);
				readToken();
			}
		}
		
		return buf.toString();
	}
	
	private void parseAnnotationFile() throws IOException, JAIFSyntaxException {
		parsePackageDefinition();
		while (!atEOF()) {
			parsePackageDefinition();
		}
	}
	
	private void parsePackageDefinition() throws IOException, JAIFSyntaxException {
		expect("package");
		Token t = peekToken();
		
		String pkgName;
		if (t.kind == TokenKind.IDENTIFIER) {
			// Hmmm....the spec says just a plain identifier here.
			// However, I'm pretty sure we want a compound name.
			pkgName = readCompoundName();
		} else {
			pkgName = "";
		}
		
		callback.startPackage(pkgName);
	}

}
