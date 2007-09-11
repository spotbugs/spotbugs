/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;

/**
 * Dataflow value representing the current nesting of
 * catch and finally blocks.  We assume that any catch block
 * with a non-empty catch type is a user catch block,
 * and any catch block with an empty catch type (i.e., catch all
 * exceptions) is a finally block.  This assumption isn't quite
 * accurate, but it seems to be a reasonable first approximation.
 *
 * <p>If valid (isValid() returns true),
 * a BlockType value is a stack of elements, which are either CATCH
 * or FINALLY values.  Call getDepth() to get the current
 * nesting depth.  Call get(int <i>n</i>) to get the
 * <i>n</i>th stack item. Call getTopValue() to get the current
 * top of the stack.</p>
 *
 * <p>If invalid (isValid() returns false),
 * a BlockType value is either <i>top</i> or <i>bottom</i>.
 * These are the special values at the top and bottom of
 * the dataflow lattice.</p>
 *
 * <p>The dataflow lattice is effectively finite-height because
 * real Java methods are guaranteed to have a finite 
 * catch and finally block nesting level.</p>
 *
 * @see BlockTypeAnalysis
 * @author David Hovemeyer
 */
public class BlockType extends BitSet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final boolean CATCH = false;
	public static final boolean FINALLY = true;

	private boolean isValid;
	private boolean isTop;
	private int depth;

	/**
	 * Constructor.
	 * Should only be called by BlockTypeAnalysis.
	 */
	BlockType() {
	}

	/**
	 * Return whether or not this value is valid,
	 * meaning it contains a valid representation of the
	 * nesting of catch and finally blocks.
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * Get the current nesting depth.
	 * The value must be valid.
	 */
	public int getDepth() {
		if (!isValid) throw new IllegalStateException();
		return depth;
	}

	/**
	 * Get the top value on the catch and finally block nesting stack.
	 */
	public boolean getTopValue() {
		if (depth == 0) throw new IllegalStateException();
		return get(depth - 1);
	}

	/**
	 * Return whether or not this value represents "normal" control-flow.
	 * Normal control flow are all blocks outside any catch or finally block.
	 */
	public boolean isNormal() {
		if (!isValid) throw new IllegalStateException();
		return getDepth() == 0;
	}

	/**
	 * Make this value represent "normal" control flow.
	 */
	public void setNormal() {
		this.isValid = true;
		this.depth = 0;
	}

	/**
	 * Return whether or not this is the special "top" dataflow value.
	 */
	public boolean isTop() {
		return !isValid && isTop;
	}

	/**
	 * Make this the special "top" dataflow value.
	 */
	public void setTop() {
		this.isValid = false;
		this.isTop = true;
	}

	/**
	 * Return whether or not this is the special "bottom" dataflow value.
	 */
	public boolean isBottom() {
		return !isValid && !isTop;
	}

	/**
	 * Make this the special "bottom" dataflow value.
	 */
	public void setBottom() {
		this.isValid = false;
		this.isTop = false;
	}

	/**
	 * Make this object an exact duplicate of given object.
	 *
	 * @param other the other BlockType object
	 */
	public void copyFrom(BlockType other) {
		this.isValid = other.isValid;
		this.isTop = other.isTop;
		if (isValid) {
			this.depth = other.depth;
			this.clear();
			this.or(other);
		}
	}

	/**
	 * Return whether or not this object is identical to the one given.
	 *
	 * @param other the other BlockType object
	 * @return true if this object is identical to the one given,
	 *         false otherwise
	 */
	public boolean sameAs(BlockType other) {
		if (!this.isValid) {
			return !other.isValid
				&& (this.isTop == other.isTop);
		} else {
			if (!other.isValid)
				return false;
			else {
				// Both facts are valid
				if (this.depth != other.depth)
					return false;

				// Compare bits
				for (int i = 0; i < this.depth; ++i) {
					if (this.get(i) != other.get(i))
						return false;
				}

				return true;
			}
		}
	}

	/**
	 * Merge other dataflow value into this value.
	 *
	 * @param other the other BlockType value
	 */
	public void mergeWith(BlockType other) {
		if (this.isTop() || other.isBottom()) {
			copyFrom(other);
		} else if (isValid()) {
			// To merge, we take the common prefix
			int pfxLen = Math.min(this.depth, other.depth);
			int commonLen;
			for (commonLen = 0; commonLen < pfxLen; ++commonLen) {
				if (this.get(commonLen) != other.get(commonLen))
					break;
			}
			this.depth = commonLen;
		}
	}

	/**
	 * Enter a catch block.
	 */
	public void pushCatch() {
		push(CATCH);
	}

	/**
	 * Enter a finally block.
	 */
	public void pushFinally() {
		push(FINALLY);
	}

	@Override
		 public String toString() {
		if (isTop())
			return "<top>";
		else if (isBottom())
			return "<bottom>";
		else {
			StringBuffer buf = new StringBuffer();
			buf.append("N");
			for (int i = 0; i < depth; ++i) {
				buf.append(get(i) == CATCH ? "C" : "F");
			}
			return buf.toString();
		}
	}

	private void push(boolean value) {
		set(depth, value);
		++depth;
	}
}

// vim:ts=4
