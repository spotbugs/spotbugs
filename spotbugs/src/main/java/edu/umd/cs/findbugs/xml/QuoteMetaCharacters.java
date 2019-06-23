/*
 * XML input/output support for FindBugs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.xml;

import java.io.IOException;

import javax.annotation.Nonnull;


/**
 * Quote metacharacters in a String.
 *
 * @see MetaCharacterMap
 * @author David Hovemeyer
 */
public abstract class QuoteMetaCharacters {
    private final String text;

    private final MetaCharacterMap map;

    /**
     * Constructor.
     *
     * @param text
     *            the text in which we want to quote metacharacters
     * @param map
     *            the MetaCharacterMap
     */
    public QuoteMetaCharacters(@Nonnull String text, @Nonnull MetaCharacterMap map) {
        if (text == null) {
            throw new NullPointerException("text must be nonnull");
        }
        if (map == null) {
            throw new NullPointerException("map must be nonnull");
        }
        this.text = text;
        this.map = map;
    }

    /**
     * Quote metacharacters in the text.
     */
    public void process() throws IOException {
        int pos = 0;
        do {
            int meta = findNextMeta(text, pos);
            if (meta >= 0) {
                emitLiteral(text.substring(pos, meta));
                emitLiteral(map.getReplacement(text.substring(meta, meta + 1)));
                pos = meta + 1;
            } else {
                emitLiteral(text.substring(pos, text.length()));
                pos = text.length();
            }
        } while (pos < text.length());
    }

    /**
     * Downcall method to emit literal text, in which any occurrences of the
     * metacharacters are quoted.
     *
     * @param s
     *            the literal text to emit
     */
    public abstract void emitLiteral(String s) throws IOException;

    private int findNextMeta(String s, int start) {
        for (int i = start; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (map.isMeta(c)) {
                return i;
            }
        }
        return -1;
    }
}
