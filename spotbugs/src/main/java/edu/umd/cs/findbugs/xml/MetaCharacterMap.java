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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Map of metacharacters that need to be escaped, and what to replace them with.
 *
 * @see QuoteMetaCharacters
 * @author David Hovemeyer
 */
public class MetaCharacterMap {
    private final BitSet metaCharacterSet;

    private final Map<String, String> replacementMap;

    /**
     * Constructor. Creates an empty object.
     */
    public MetaCharacterMap() {
        this.metaCharacterSet = new BitSet();
        this.replacementMap = new HashMap<>();
    }

    /**
     * Add a metacharacter and its replacement.
     *
     * @param meta
     *            the metacharacter
     * @param replacement
     *            the String to replace the metacharacter with
     */
    public void addMeta(char meta, String replacement) {
        metaCharacterSet.set(meta);
        replacementMap.put(new String(new char[] { meta }), replacement);
    }

    /**
     * Return whether or not given character is a metacharacter.
     */
    boolean isMeta(char c) {
        return metaCharacterSet.get(c);
    }

    /**
     * Get the replacement for a metacharacter.
     *
     * @param c
     *            a String containing the metacharacter
     */
    String getReplacement(String c) {
        return replacementMap.get(c);
    }
}
