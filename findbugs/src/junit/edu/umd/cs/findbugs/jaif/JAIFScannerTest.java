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
		JAIFScanner scanner = getScanner("  \t  \t\t@foobar Baz123   ( Boing Boing) @Yum@Yum __123  $plotz");

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

	public void testScanFloatingPointLiteral() throws Exception {
		JAIFScanner scanner = getScanner("1e1f    2.f     .3f     0f      3.14f   6.022137e+23f");

		checkToken(scanner, "1e1f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "2.f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, ".3f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "0f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "3.14f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "6.022137e+23f", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
	}

	public void testScanFloatingPointLiteral2() throws Exception {
		JAIFScanner scanner = getScanner("1e1     2.      .3      0.0     3.14    1e-9d   1e137");

		checkToken(scanner, "1e1", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "2.", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, ".3", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "0.0", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "3.14", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "1e-9d", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "1e137", JAIFTokenKind.FLOATING_POINT_LITERAL);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
	}

	public void testScanOctalLiteral() throws Exception {
		JAIFScanner scanner = getScanner("0237   01575L  027365l");

		checkToken(scanner, "0237", JAIFTokenKind.OCTAL_LITERAL);
		checkToken(scanner, "01575L", JAIFTokenKind.OCTAL_LITERAL);
		checkToken(scanner, "027365l", JAIFTokenKind.OCTAL_LITERAL);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
	}

	public void testScanHexLiteral() throws Exception {
		JAIFScanner scanner = getScanner("0xDEADbeef   0xcafeBabeL   0X123EEfl");

		checkToken(scanner, "0xDEADbeef", JAIFTokenKind.HEX_LITERAL);
		checkToken(scanner, "0xcafeBabeL", JAIFTokenKind.HEX_LITERAL);
		checkToken(scanner, "0X123EEfl", JAIFTokenKind.HEX_LITERAL);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
	}

	public void testScanDecimalLiteral() throws Exception {
		JAIFScanner scanner = getScanner("1234     5678L    91919191l");
		
		checkToken(scanner, "1234", JAIFTokenKind.DECIMAL_LITERAL);
		checkToken(scanner, "5678L", JAIFTokenKind.DECIMAL_LITERAL);
		checkToken(scanner, "91919191l", JAIFTokenKind.DECIMAL_LITERAL);
		checkToken(scanner, "\n", JAIFTokenKind.NEWLINE);
	}
}
