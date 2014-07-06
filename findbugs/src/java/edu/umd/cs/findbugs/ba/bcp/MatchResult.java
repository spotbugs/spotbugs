/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.bcp;

/**
 * The result of matching a single PatternElement against a single instruction.
 * Specifies the PatternElement and the (possibly updated) BindingSet. The
 * reason we need this class is because some kinds of PatternElements, such as
 * MatchAny, may use it to indicate that a child PatternElement was the one that
 * actually matched the instruction.
 *
 * @author David Hovemeyer
 * @see PatternElement
 * @see BindingSet
 */
public class MatchResult {
    private final PatternElement patternElement;

    private final BindingSet bindingSet;

    /**
     * Constructor.
     *
     * @param patternElement
     *            the PatternElement that matched the instruction
     * @param bindingSet
     *            the possibly updated BindingSet
     */
    public MatchResult(PatternElement patternElement, BindingSet bindingSet) {
        this.patternElement = patternElement;
        this.bindingSet = bindingSet;
    }

    /**
     * Get the PatternElement.
     */
    public PatternElement getPatternElement() {
        return patternElement;
    }

    /**
     * Get the BindingSet.
     */
    public BindingSet getBindingSet() {
        return bindingSet;
    }

}

