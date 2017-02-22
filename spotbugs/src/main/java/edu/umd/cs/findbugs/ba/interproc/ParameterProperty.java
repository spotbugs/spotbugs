/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.ba.interproc;

import java.util.BitSet;
import java.util.Iterator;

/**
 * Method property recording which parameters are have some property
 * (originally, which were required to be nonnull, now made more generic)
 *
 * @author David Hovemeyer
 */
public class ParameterProperty {
    /**
     * Maximum number of parameters that can be represented by a
     * ParameterProperty.
     */
    public static final int MAX_PARAMS = 32;

    private int bits;

    /**
     * Constructor. Parameters are all assumed not to be non-null.
     */
    public ParameterProperty() {
        this.bits = 0;
    }

    /**
     * Constructor. Parameters are all assumed not to be non-null.
     */
    public ParameterProperty(int bits) {
        this.bits = bits;
    }

    /**
     * Get the non-null param bitset.
     *
     * @return the non-null param bitset
     */
    public int getParamsWithProperty() {
        return bits;
    }

    public Iterable<Integer> iterable() {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int nextInt = 0;
                    {
                        advanceNextInt();
                    }

                    private void advanceNextInt() {
                        while (!hasProperty(nextInt) && nextInt < 32) {
                            nextInt++;
                        }
                        if (nextInt >= 32) {
                            nextInt = -1;
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return nextInt >= 0;
                    }

                    @Override
                    public Integer next() {
                        int result = nextInt;
                        nextInt++;
                        advanceNextInt();
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();

                    }
                };
            }

        };
    }

    /**
     * Set the non-null param bitset.
     *
     * @param nonNullParamSet
     *            the non-null param bitset
     */
    public void setParamsWithProperty(int nonNullParamSet) {
        this.bits = nonNullParamSet;
    }

    /**
     * Set the non-null param set from given BitSet.
     *
     * @param nonNullSet
     *            BitSet indicating which parameters are non-null
     */
    public void setParamsWithProperty(BitSet nonNullSet) {
        for (int i = 0; i < 32; ++i) {
            setParamWithProperty(i, nonNullSet.get(i));
        }
    }

    /**
     * Set whether or not a parameter might be non-null.
     *
     * @param param
     *            the parameter index
     * @param hasProperty
     *            true if the parameter might be non-null, false otherwise
     */
    public void setParamWithProperty(int param, boolean hasProperty) {
        if (param < 0 || param > 31) {
            return;
        }
        if (hasProperty) {
            bits |= (1 << param);
        } else {
            bits &= ~(1 << param);
        }
    }

    /**
     * Return whether or not a parameter might be non-null.
     *
     * @param param
     *            the parameter index
     * @return true if the parameter might be non-null, false otherwise
     */
    public boolean hasProperty(int param) {
        if (param < 0 || param > 31) {
            return false;
        } else {
            return (bits & (1 << param)) != 0;
        }
    }

    /**
     * Given a bitset of null arguments passed to the method represented by this
     * property, return a bitset indicating which null arguments correspond to
     * an non-null param.
     *
     * @param nullArgSet
     *            bitset of null arguments
     * @return bitset intersecting null arguments and non-null params
     */
    public BitSet getMatchingParameters(BitSet nullArgSet) {
        BitSet result = new BitSet();
        for (int i = 0; i < 32; ++i) {
            result.set(i, nullArgSet.get(i) && hasProperty(i));
        }
        return result;
    }

    public BitSet getAsBitSet() {
        BitSet result = new BitSet();
        if (isEmpty()) {
            return result;
        }
        for (int i = 0; i < 32; ++i) {
            result.set(i, hasProperty(i));
        }
        return result;
    }

    /**
     * Return whether or not the set of non-null parameters is empty.
     *
     * @return true if the set is empty, false if it contains at least one
     *         parameter
     */
    public boolean isEmpty() {
        return bits == 0;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append('{');
        for (int i = 0; i < 32; ++i) {
            if (hasProperty(i)) {
                if (buf.length() > 1) {
                    buf.append(',');
                }
                buf.append(i);
            }
        }
        buf.append('}');

        return buf.toString();
    }

    /**
     * Intersect this set with the given set. Useful for summarizing the
     * properties of multiple methods.
     *
     * @param targetDerefParamSet
     *            another set
     */
    public void intersectWith(ParameterProperty targetDerefParamSet) {
        bits &= targetDerefParamSet.bits;
    }

    /**
     * Make this object the same as the given one.
     *
     * @param other
     *            another ParameterNullnessProperty
     */
    public void copyFrom(ParameterProperty other) {
        this.bits = other.bits;
    }
}
