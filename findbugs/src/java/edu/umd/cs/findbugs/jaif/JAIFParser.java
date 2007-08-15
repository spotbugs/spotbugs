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
	private JAIFScanner scanner;
	private JAIFEvents callback;
	private JAIFToken next;
	
	public JAIFParser(Reader reader, JAIFEvents callback) {
		this.scanner = new JAIFScanner(reader);
		this.callback = callback;
	}
	
	public void parse() throws IOException, JAIFSyntaxException {
		parseAnnotationFile();
	}

	int getLineNumber() {
		return scanner.getLineNumber();
	}
	
	private boolean atEOF() throws IOException {
		return scanner.atEOF();
	}
	
	private JAIFToken expect(String s) throws IOException, JAIFSyntaxException {
		JAIFToken t = scanner.nextToken();
		if (!t.lexeme.equals(s)) {
			throw new JAIFSyntaxException(this, "Unexpected token " + t + " (was expecting " + s + ")");
		}
		return t;
	}
	
	private JAIFToken expect(JAIFTokenKind kind) throws IOException, JAIFSyntaxException {
		JAIFToken t = scanner.nextToken();
		if (t.kind != kind) {
			throw new JAIFSyntaxException(this, "Unexpected token " + t + " (was expecting a " + kind.toString() + ")");
		}
		return t;
	}

	private String readCompoundName() throws IOException, JAIFSyntaxException {
		StringBuffer buf = new StringBuffer();
		
		boolean firstToken = true;
		
		while (true) {
			JAIFToken t = scanner.nextToken();
			assert t.kind == JAIFTokenKind.IDENTIFIER_OR_KEYWORD;

			if (firstToken) {
				firstToken = false;
			} else if (t.lexeme.startsWith("@")) {
				throw new JAIFSyntaxException(this, "Illegal compound name (unexpected '@' character)");
			}
			
			buf.append(t.lexeme);

			t = scanner.peekToken();
			if (t.kind != JAIFTokenKind.DOT) {
				break;
			} else {
				buf.append(t.lexeme);
				scanner.nextToken();
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
		JAIFToken t = scanner.peekToken();
		
		String pkgName;
		if (t.kind == JAIFTokenKind.IDENTIFIER_OR_KEYWORD) {
			// Hmmm....the spec says just a plain identifier here.
			// However, I'm pretty sure we want a compound name.
			pkgName = readCompoundName();
		} else {
			pkgName = "";
		}
		
		callback.startPackageDefinition(pkgName);
		
		expect(":");
		
		t = scanner.peekToken();
		while (t.isStartOfAnnotationName()) {
			parseAnnotation();
		}
		
		//
		// TODO: more
		//
		
		callback.endPackageDefinition(pkgName);
	}
	
	private void parseAnnotation() throws IOException, JAIFSyntaxException {
		String annotationName = readCompoundName();
		assert annotationName.startsWith("@");
		
		callback.startAnnotation(annotationName);
		
		JAIFToken t = scanner.peekToken();
		if (t.kind == JAIFTokenKind.LPAREN) {
			parseAnnotationField();
			t = scanner.peekToken();
			while (t.kind != JAIFTokenKind.RPAREN) {
				expect(",");
				parseAnnotationField();
				t = scanner.peekToken();
			}
			assert t.kind == JAIFTokenKind.RPAREN;
			scanner.nextToken();
		}
		
		callback.endAnnotation(annotationName);
	}

	private void parseAnnotationField() throws IOException, JAIFSyntaxException {
		JAIFToken id = expect(JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
		expect("=");
		Object constant = parseConstant();
		
		callback.annotationField(id.lexeme, constant);
	}

	private Object parseConstant() {
		// TODO Auto-generated method stub
		return null;
	}

}
