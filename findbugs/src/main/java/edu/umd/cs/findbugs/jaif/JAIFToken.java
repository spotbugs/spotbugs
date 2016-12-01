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

/**
 * One lexical token of an external annotations file.
 *
 * @author David Hovemeyer
 * @see <a href="http://pag.csail.mit.edu/jsr308/annotation-file-utilities/">
 * http://pag.csail.mit.edu/jsr308/annotation-file-utilities/</a>
 */
class JAIFToken {
    JAIFTokenKind kind;

    String lexeme;

    //    int lineNum;

    public JAIFToken(JAIFTokenKind kind, String lexeme, int lineNum) {
        this.kind = kind;
        this.lexeme = lexeme;
        //        this.lineNum = lineNum;
        // System.out.println("token: " + this);
    }

    public boolean isStartOfAnnotationName() {
        return lexeme.startsWith("@");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if ("\n".equals(lexeme)) {
            return "<newline>";
        } else {
            return lexeme;
        }
    }
}
