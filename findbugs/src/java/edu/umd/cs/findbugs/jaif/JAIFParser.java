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

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Locale;

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
			throw new JAIFSyntaxException(this, "Unexpected token " + t + " (was expecting a `" + kind.toString() + "' token)");
		}
		return t;
	}
	
	private void expectEndOfLine() throws IOException, JAIFSyntaxException {
		// extract-annotations seems to sometimes produce multiple newlines where
		// the grammar indicates that only one will appear.
		// So, we treat any sequence of one or more newlines as one newline.
		int nlCount = 0;
		JAIFToken t;

		while (true) {
			if (scanner.atEOF()) {
				t = null;
				break;
			}
			
			t = scanner.peekToken();
			if (t.kind != JAIFTokenKind.NEWLINE) {
				break;
			}
			
			++nlCount;
			scanner.nextToken();
		}
		
		if (nlCount < 1) {
			String msg = (t == null) ? "Unexpected end of file" : "Unexpected token " + t + " (was expecting <newline>)";
			throw new JAIFSyntaxException(this, msg);
		}
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
	
	private String readType() throws IOException, JAIFSyntaxException {
		StringBuffer buf = new StringBuffer();
		
		JAIFToken t = expect(JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
		
		if (t.lexeme.equals("enum")) {
			
		}
		
		return buf.toString();
	}
	
	private void parseAnnotationFile() throws IOException, JAIFSyntaxException {
		parsePackageDefinition();
		while (!scanner.atEOF()) {
			parsePackageDefinition();
		}
	}
	
	private void parsePackageDefinition() throws IOException, JAIFSyntaxException {
		expect("package");
		JAIFToken t = scanner.peekToken();
		
		String pkgName;
		
		if (t.kind != JAIFTokenKind.NEWLINE) {
			// Optional package name and package-level annotations
			
			// Hmmm....the spec says just a plain identifier here.
			// However, I'm pretty sure we want a compound name.
			pkgName = readCompoundName();

			expect(":");

			t = scanner.peekToken();
			while (t.isStartOfAnnotationName()) {
				parseAnnotation();
			}
		} else {
			pkgName = ""; // default package
		}
		
		expectEndOfLine();

		callback.startPackageDefinition(pkgName);

		while (!scanner.atEOF()) {
			t = scanner.peekToken();
			if (t.lexeme.equals("package")) {
				break;
			}
			
			parseAnnotationDefinitionOrClassDefinition();
		}
		
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

	private Object parseConstant() throws IOException, JAIFSyntaxException {
		JAIFToken t = scanner.peekToken();
		
		switch (t.kind) {
		case IDENTIFIER_OR_KEYWORD:
			// This is an enum constant specified by a compound name.
			// Represent it as a JAIFEnumConstant object.
			String name = readCompoundName();
			return new JAIFEnumConstant(name);
		case DECIMAL_LITERAL:
			t = scanner.nextToken();
			return Integer.parseInt(t.lexeme);
		case OCTAL_LITERAL:
			t = scanner.nextToken();
			return Integer.parseInt(t.lexeme, 8);
		case HEX_LITERAL:
			t = scanner.nextToken();
			return Integer.parseInt(t.lexeme, 16);
		case FLOATING_POINT_LITERAL:
			t = scanner.nextToken();
			boolean isFloat = t.lexeme.toLowerCase(Locale.ENGLISH).endsWith("f");
			if (isFloat) {
				return Float.parseFloat(t.lexeme);
			} else {
				return Double.parseDouble(t.lexeme);
			}
		case STRING_LITERAL:
			t = scanner.nextToken();
			return unparseStringLiteral(t.lexeme);
		default:
			throw new JAIFSyntaxException(this, "Illegal constant");
		}
	}

	private Object unparseStringLiteral(String lexeme) {
		StringBuffer buf = new StringBuffer();

		int where = 1; // skip initial double quote char

		while (true) {
			assert where < lexeme.length();
			char c = lexeme.charAt(where); 

			if (c == '"') {
				break;
			}

			if (c != '\\') {
				buf.append(c);
				where++;
				continue;
			}

			where++;
			assert where < lexeme.length();
			
			c = lexeme.charAt(where);
			switch (c) {
			case 'b':
				buf.append('\b'); where++; break;
			case 't':
				buf.append('\t'); where++; break;
			case 'n':
				buf.append('\n'); where++; break;
			case 'f':
				buf.append('\t'); where++; break;
			case 'r':
				buf.append('\r'); where++; break;
			case '"':
				buf.append('"'); where++; break;
			case '\'':
				buf.append('\''); where++; break;
			case '\\':
				buf.append('\\'); where++; break;
			default:
				char value = (char) 0;
				while (c >= '0' && c <= '7') {
					value *= 8;
					value += (c - '0');
					where++;
					assert where < lexeme.length();
					c = lexeme.charAt(where);
				}
				buf.append(value);
			}
		}

		return buf.toString();
	}
	
	private void parseAnnotationDefinitionOrClassDefinition() throws IOException, JAIFSyntaxException {
		JAIFToken t = scanner.peekToken();
		
		if (t.lexeme.equals("annotation")) {
			parseAnnotationDefinition();
		} else if (t.lexeme.equals("class")) {
			parseClassDefinition();
		} else {
			throw new JAIFSyntaxException(this, "Unexpected token " + t + " (expected `annotation' or `class')");
		}
	}
	
	private void parseAnnotationDefinition() throws IOException, JAIFSyntaxException {
		expect("annotation");
		
		String retention = null;
		
		JAIFToken t = scanner.peekToken();
		if (t.lexeme.equals("visible") || t.lexeme.equals("invisible") || t.lexeme.equals("source")) {
			retention = t.lexeme;
			scanner.nextToken();
		}
		
		String annotationName = expect(JAIFTokenKind.IDENTIFIER_OR_KEYWORD).lexeme;
		
		expect(JAIFTokenKind.COLON);
		expectEndOfLine();
		
		callback.startAnnotationDefinition(annotationName, retention);
		
		t = scanner.peekToken();
		while (t.kind != JAIFTokenKind.NEWLINE) {
			parseAnnotationFieldDefinition();
		}
	}
	
	private void parseAnnotationFieldDefinition() throws IOException, JAIFSyntaxException {
		String type = readType();
		String fieldName = expect(JAIFTokenKind.IDENTIFIER_OR_KEYWORD).lexeme;
		
		callback.annotationFieldDefinition(type, fieldName);
	}

	private void parseClassDefinition() {
		
	}

	public static void main(String[] args) throws Exception {
	    if (args.length != 1) {
	    	System.err.println("Usage: " + JAIFParser.class.getName() + " <jaif file>");
	    	System.exit(1);
	    }
	    
	    JAIFEvents callback = new JAIFEvents() {
	    	public void annotationField(String fieldName, Object constant) {
	    		System.out.println("    " + fieldName + "=" + constant);
	    	}

	    	public void endAnnotation(String annotationName) {
	    	}

	    	public void endPackageDefinition(String pkgName) {
	    	}

	    	public void startAnnotation(String annotationName) {
	    		System.out.println("  annotation " + annotationName);
	    	}

	    	public void startPackageDefinition(String pkgName) {
	    		System.out.println("package " + pkgName);
	    	}
	    	
	    	public void startAnnotationDefinition(String annotationName, String retention) {
	    		System.out.println("  annotation " + annotationName + " " + retention);
	    	}
	    	
	    	public void endAnnotationDefinition(String annotationName) {
	    	}
	    	
	    	public void annotationFieldDefinition(String type, String fieldName) {
	    		System.out.println("    " + type + " " + fieldName);
	    	}
	    };
    	
    	JAIFParser parser = new JAIFParser(new FileReader(args[0]), callback);
    	parser.parse();
    }
}
