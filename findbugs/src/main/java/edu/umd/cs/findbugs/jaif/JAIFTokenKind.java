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

public enum JAIFTokenKind {
    NEWLINE("<newline>"), COLON(":"), DOT("."), IDENTIFIER_OR_KEYWORD("<identifier or keyword>"), LPAREN("("), RPAREN(")"), COMMA(
            ","), EQUALS("="), OCTAL_LITERAL("<octal literal>"), DECIMAL_LITERAL("<decimal literal>"), HEX_LITERAL(
                    "<hex literal>"), FLOATING_POINT_LITERAL("<floating point literal>"), STRING_LITERAL("<string literal>");

    private String stringRep;

    private JAIFTokenKind(String stringRep) {
        this.stringRep = stringRep;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return stringRep;
    }
}
