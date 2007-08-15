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

import java.io.StringReader;

import junit.framework.TestCase;

public class JAIFScannerTest extends TestCase {
	
	private JAIFScanner getScanner(String text) {
		return new JAIFScanner(new StringReader(text));
	}
	
	private void checkToken(JAIFScanner scanner, String lexeme, JAIFTokenKind kind) throws Exception {
		JAIFToken t = scanner.nextToken();
		
		assertEquals(lexeme, t.lexeme);
		assertEquals(kind, t.kind);
	}
	
	public void testScanColon() throws Exception {
		JAIFScanner scanner = getScanner(":");
		checkToken(scanner, ":", JAIFTokenKind.COLON);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
    }
	
	public void testScanParens() throws Exception {
		JAIFScanner scanner = getScanner("()");

		checkToken(scanner, "(", JAIFTokenKind.LPAREN);
		checkToken(scanner, ")", JAIFTokenKind.RPAREN);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
    }
	
	public void testScanComma() throws Exception {
	    JAIFScanner scanner = getScanner(",");
	    
	    checkToken(scanner, ",", JAIFTokenKind.COMMA);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
    }
	
	public void testScanEquals() throws Exception {
		JAIFScanner scanner = getScanner("=");
		
		checkToken(scanner, "=", JAIFTokenKind.EQUALS);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
    }

	public void testScanIdentifier() throws Exception {
	    JAIFScanner scanner = getScanner("@foobar Baz123   ( Boing Boing) @Yum@Yum __123  $plotz");
	    
	    checkToken(scanner, "@foobar", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "Baz123", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "(", JAIFTokenKind.LPAREN);
	    checkToken(scanner, "Boing", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "Boing", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, ")", JAIFTokenKind.RPAREN);
	    checkToken(scanner, "@Yum", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "@Yum", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "__123", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
	    checkToken(scanner, "$plotz", JAIFTokenKind.IDENTIFIER_OR_KEYWORD);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
    }
}
