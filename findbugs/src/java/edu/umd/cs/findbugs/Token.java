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

/**
 * Simple token class.
 *
 * @author David Hovemeyer
 * @see Tokenizer
 */
public class Token {
    /**
     * End of file.
     */
    public static final int EOF = -1;

    /**
     * End of line.
     */
    public static final int EOL = -2;

    /**
     * An ordinary word, number, etc.
     */
    public static final int WORD = 0;

    /**
     * A string or character literal.
     */
    public static final int STRING = 1;

    /**
     * A single character token.
     */
    public static final int SINGLE = 2;

    /**
     * A comment.
     */
    public static final int COMMENT = 3;

    private final int kind;

    private final String lexeme;

    /**
     * Constructor.
     *
     * @param kind
     *            the kind of token
     * @param lexeme
     *            the text value of the token
     */
    public Token(int kind, String lexeme) {
        this.kind = kind;
        this.lexeme = lexeme;
    }

    /**
     * Constructor when there is no text. E.g., EOF and EOL.
     *
     * @param kind
     *            the kind of token
     */
    public Token(int kind) {
        this.kind = kind;
        this.lexeme = "";
    }

    /**
     * Get the kind of token.
     */
    public int getKind() {
        return kind;
    }

    /**
     * Get the text value of the token.
     */
    public String getLexeme() {
        return lexeme;
    }
}

