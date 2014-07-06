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

package edu.umd.cs.findbugs.ba;

import java.util.Collection;
import java.util.HashSet;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFactory;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Lock counts for values (as produced by ValueNumberAnalysis). A LockSet tells
 * us the lock counts for all values in a method, insofar as we can accurately
 * determine them.
 *
 * @author David Hovemeyer
 * @see edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysis
 */
public final class LockSet {
    /**
     * An uninitialized lock value.
     */
    public static final int TOP = -1;

    /**
     * An invalid lock count resulting from the meet of two different
     * (inconsistent) lock counts.
     */
    public static final int BOTTOM = -2;

    private static final int INVALID = -1;

    private static final int DEFAULT_CAPACITY = 8;

    /**
     * Lock counts are stored in an array. Even indices <i>i</i> are value
     * numbers of lock objects. Odd indices <i>i+1</i> are lock counts. This
     * representation is fairly compact in memory.
     */
    private int[] array;

    /**
     * The lock count value to return for nonexistent lock entries.
     */
    private int defaultLockCount;

    /**
     * Constructor. Creates an empty lock set which returns TOP for nonexistent
     * lock entries.
     */
    public LockSet() {
        this.array = new int[DEFAULT_CAPACITY];
        this.defaultLockCount = TOP;
        clear();
    }

    /**
     * Get the lock count for given lock object.
     *
     * @param valueNumber
     *            value number of the lock object
     * @return the lock count for the lock object
     */
    public int getLockCount(int valueNumber) {
        int index = findIndex(valueNumber);
        if (index < 0) {
            return defaultLockCount;
        } else {
            return array[index + 1];
        }
    }

    public boolean isTop() {
        return defaultLockCount == TOP;
    }

    /**
     * Set the lock count for a lock object.
     *
     * @param valueNumber
     *            value number of the lock object
     * @param lockCount
     *            the lock count for the lock
     */
    public void setLockCount(int valueNumber, int lockCount) {
        int index = findIndex(valueNumber);
        if (index < 0) {
            addEntry(index, valueNumber, lockCount);
        } else {
            array[index + 1] = lockCount;
        }
    }

    /**
     * Set the default lock count to return for nonexistent lock entries.
     *
     * @param defaultLockCount
     *            the default lock count value
     */
    public void setDefaultLockCount(int defaultLockCount) {
        this.defaultLockCount = defaultLockCount;
    }

    /**
     * Get the number of distinct lock values with positive lock counts.
     */
    public int getNumLockedObjects() {
        int result = 0;
        for (int i = 0; i < array.length; i += 2) {
            if (array[i] == INVALID) {
                break;
            }
            if (array[i + 1] > 0) {
                ++result;
            }
        }
        return result;
    }

    /**
     * Make this LockSet the same as the given one.
     *
     * @param other
     *            the LockSet to copy
     */
    public void copyFrom(LockSet other) {
        if (other.array.length != array.length) {
            array = new int[other.array.length];
        }
        System.arraycopy(other.array, 0, array, 0, array.length);
        this.defaultLockCount = other.defaultLockCount;
    }

    /**
     * Clear all entries out of this LockSet.
     */
    public void clear() {
        for (int i = 0; i < array.length; i += 2) {
            array[i] = INVALID;
        }
    }

    /**
     * Meet this LockSet with another LockSet, storing the result in this
     * object.
     *
     * @param other
     *            the other LockSet
     */
    public void meetWith(LockSet other) {
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                break;
            }

            int mine = array[i + 1];
            int his = other.getLockCount(valueNumber);
            array[i + 1] = mergeValues(mine, his);
        }

        for (int i = 0; i < other.array.length; i += 2) {
            int valueNumber = other.array[i];
            if (valueNumber < 0) {
                break;
            }

            int mine = getLockCount(valueNumber);
            int his = other.array[i + 1];
            setLockCount(valueNumber, mergeValues(mine, his));
        }

        setDefaultLockCount(0);
    }

    /**
     * Return whether or not this LockSet is the same as the one given.
     *
     * @param other
     *            the other LockSet
     */
    public boolean sameAs(LockSet other) {
        return this.identicalSubset(other) && other.identicalSubset(this);
    }

    /**
     * Determine whether or not this lock set contains any locked values which
     * are method return values.
     *
     * @param factory
     *            the ValueNumberFactory that produced the lock values
     */
    public boolean containsReturnValue(ValueNumberFactory factory) {
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                break;
            }
            int lockCount = array[i + 1];
            if (lockCount > 0 && factory.forNumber(valueNumber).hasFlag(ValueNumber.RETURN_VALUE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Destructively intersect this lock set with another. Note that this is
     * <em>not</em> a dataflow merge: we are interested in finding out which
     * locks are held in both sets, not in the exact lock counts.
     *
     * @param other
     *            the other LockSet
     */
    public void intersectWith(LockSet other) {
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                break;
            }
            int myLockCount = array[i + 1];
            if (myLockCount <= 0) {
                continue;
            }
            int otherLockCount = other.getLockCount(valueNumber);
            if (otherLockCount <= 0) {
                /* This set holds the lock, but the other one doesn't. */
                array[i + 1] = 0;
            }
        }
    }

    /**
     * Return whether or not this lock set is empty, meaning that no locks have
     * a positive lock count.
     *
     * @return true if no locks are held, false if at least one lock is held
     */
    public boolean isEmpty() {
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                return true;
            }
            int myLockCount = array[i + 1];
            if (myLockCount > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean identicalSubset(LockSet other) {
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                break;
            }
            int mine = array[i + 1];
            int his = other.getLockCount(valueNumber);
            if (mine != his)
            {
                return false;
                // System.out.println("For value " + valueNumber + ", " + mine +
                // "==" + his);
            }
        }
        return true;
    }

    private static int mergeValues(int a, int b) {
        if (a == TOP) {
            return b;
        } else if (b == TOP) {
            return a;
        } else if (a == BOTTOM || b == BOTTOM) {
            return BOTTOM;
        } else if (a == b) {
            return a;
        } else {
            return BOTTOM;
        }
    }

    private int findIndex(int valueNumber) {
        for (int i = 0; i < array.length; i += 2) {
            int value = array[i];
            if (value < 0) {
                return -(i + 1); // didn't find requested valueNumber - return
            } else if (value == valueNumber)
            {
                return i; // found requested valueNumber
            }
        }
        return -(array.length + 1); // didn't find requested valueNumber, and
        // array is full
    }

    private void addEntry(int negatedIndex, int valueNumber, int lockCount) {
        int index = -(negatedIndex + 1);
        int origCapacity = array.length;
        if (index == origCapacity) {
            // Grow the array.

            // Allocate twice the space of the old array
            int[] data = new int[origCapacity * 2];

            // Copy existing data
            System.arraycopy(array, 0, data, 0, origCapacity);

            // Clear entries in the new part of the array
            // that won't be used to store the entry that's
            // being added
            for (int i = index + 2; i < data.length; i += 2) {
                data[i] = INVALID;
            }

            // Now we're ready to use the new array
            array = data;
        }

        array[index] = valueNumber;
        array[index + 1] = lockCount;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        boolean first = true;
        if (defaultLockCount == 0) {
            buf.append("default=0");
            first = false;
        }
        for (int i = 0; i < array.length; i += 2) {
            int valueNumber = array[i];
            if (valueNumber < 0) {
                continue;
            }
            int lockCount = array[i + 1];
            if (lockCount == 0) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(valueNumber);
            buf.append('=');
            if (lockCount == TOP) {
                buf.append("TOP");
            } else if (lockCount == BOTTOM) {
                buf.append("BOTTOM");
            } else {
                buf.append(lockCount);
            }
        }
        buf.append(']');
        return buf.toString();
    }

    /**
     * @param frame
     * @return a set of the locked value numbers
     */
    public Collection<ValueNumber> getLockedValueNumbers(ValueNumberFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("Null Frame");
        }
        HashSet<ValueNumber> result = new HashSet<ValueNumber>();
        for (ValueNumber v : frame.allSlots()) {
            if (v != null && getLockCount(v.getNumber()) > 0) {
                result.add(v);
            }
        }
        return result;
    }

    /*
     * public static void main(String[] argv) { LockSet l = new LockSet();
     * l.setLockCount(0, 1); System.out.println(l); l.setLockCount(0, 2);
     * System.out.println(l); l.setLockCount(15, 1); System.out.println(l);
     * LockSet ll = new LockSet(); ll.setLockCount(0, 1); ll.setLockCount(15,
     * 1); ll.setLockCount(69, 3); LockSet tmp = new LockSet();
     * tmp.copyFrom(ll); ll.meetWith(l); System.out.println(l + " merge with " +
     * tmp + " ==> " + ll);
     *
     * LockSet dup = new LockSet(); dup.copyFrom(ll); System.out.println(ll +
     * " == " + dup + " ==> " + ll.sameAs(dup)); System.out.println(ll + " == "
     * + l + " ==> " + ll.sameAs(l)); }
     */
}

