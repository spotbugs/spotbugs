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
 * A ByteCodePattern is a pattern matching a sequence of bytecode instructions.
 *
 * @author David Hovemeyer
 * @see PatternElement
 * @see PatternMatcher
 */
public class ByteCodePattern {
	private PatternElement first, last;
	private int interElementWild;
	private int numElements;
	private int dummyVariableCount;

	/**
	 * Add a PatternElement to the end of the pattern.
	 *
	 * @param element the PatternElement
	 * @return this object
	 */
	public ByteCodePattern add(PatternElement element) {
		if (first != null)
			addInterElementWild();
		addElement(element);
		return this;
	}

	/**
	 * Add a wildcard to match between 0 and given number of instructions.
	 * If there is already a wildcard at the end of the current pattern,
	 * resets its max value to that given.
	 *
	 * @param numWild maximum number of instructions to be matched by
	 *                the wildcard
	 */
	public ByteCodePattern addWild(int numWild) {
		Wild wild = isLastWild();
		if (wild != null)
			wild.setMinAndMax(0, numWild);
		else
			addElement(new Wild(numWild));
		return this;
	}

	/**
	 * Set number of inter-element wildcards to create between
	 * explicit PatternElements.  By default, no implicit wildcards
	 * are created.
	 *
	 * @param numWild the number of wildcard instructions which
	 *                may be matched between explicit PatternElements
	 * @return this object
	 */
	public ByteCodePattern setInterElementWild(int numWild) {
		this.interElementWild = numWild;
		return this;
	}

	/**
	 * Get the first PatternElement in the pattern.
	 */
	public PatternElement getFirst() {
		return first;
	}

	/**
	 * Get a dummy variable name.
	 * The name returned will begin with the <code>'$'</code> character,
	 * and will be different than any previous dummy variable name allocated
	 * by this object.  Dummy variable names are useful for creating
	 * PatternElements where you don't care whether the value it uses
	 * is the same as one used by another PatternElement.
	 */
	public String dummyVariable() {
		StringBuilder buf = new StringBuilder();
		buf.append("$_");
		buf.append(dummyVariableCount++);
		return buf.toString();
	}

	private void addInterElementWild() {
		if (interElementWild > 0 && isLastWild() == null)
			addElement(new Wild(interElementWild));
	}

	private void addElement(PatternElement element) {
		element.setIndex(numElements++);
		if (first == null) {
			first = last = element;
		} else {
			last.setNext(element);
			last = element;
		}
	}

	private Wild isLastWild() {
		if (last != null && last instanceof Wild)
			return (Wild) last;
		else
			return null;
	}
}

// vim:ts=4
