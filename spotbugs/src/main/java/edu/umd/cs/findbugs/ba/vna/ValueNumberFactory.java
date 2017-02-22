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

package edu.umd.cs.findbugs.ba.vna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Factory for ValueNumbers. A single Factory must be used to create all of the
 * ValueNumbers for a method.
 *
 * @author David Hovemeyer
 * @see ValueNumber
 */
public class ValueNumberFactory {
    /**
     * Store all allocated value numbers.
     */
    private ArrayList<ValueNumber> allocatedValueList = new ArrayList<ValueNumber>();

    private final HashMap<String, ValueNumber> classObjectValueMap = new HashMap<String, ValueNumber>();

    /**
     * Create a fresh (unique) value number.
     */
    public ValueNumber createFreshValue() {
        ValueNumber result = ValueNumber.createValueNumber(getNumValuesAllocated());
        allocatedValueList.add(result);
        return result;
    }

    public ValueNumber createFreshValue(int flags) {
        ValueNumber result = ValueNumber.createValueNumber(getNumValuesAllocated(), flags);
        allocatedValueList.add(result);
        return result;
    }

    /**
     * Return a previously allocated value.
     */
    public ValueNumber forNumber(int number) {
        if (number >= getNumValuesAllocated()) {
            throw new IllegalArgumentException("Value " + number + " has not been allocated");
        }
        return allocatedValueList.get(number);
    }

    /**
     * Get the number of values which have been created.
     */
    public int getNumValuesAllocated() {
        return allocatedValueList.size();
    }

    /**
     * Compact the value numbers produced by this factory.
     *
     * @param map
     *            array mapping old numbers to new numbers
     * @param numValuesAllocated
     *            the number of values allocated in the new numbering
     */
    @Deprecated
    public void compact(int[] map, int numValuesAllocated) {
        if (true) {
            throw new UnsupportedOperationException();
        }
        ArrayList<ValueNumber> oldList = this.allocatedValueList;
        ArrayList<ValueNumber> newList = new ArrayList<ValueNumber>(Collections.<ValueNumber> nCopies(numValuesAllocated, null));

        for (ValueNumber value : oldList) {
            int newNumber = map[value.getNumber()];
            if (newNumber >= 0) {
                // Note: because we are simply assigning new numbers to the
                // old ValueNumber objects, their flags remain valid.
                // value.number = newNumber;
                newList.set(newNumber, value);
            }
        }

        this.allocatedValueList = newList;
    }

    /**
     * Get the ValueNumber for given class's Class object.
     *
     * @param className
     *            the class
     */
    public ValueNumber getClassObjectValue(@DottedClassName String className) {
        // assert className.indexOf('.') == -1;
        // TODO: Check to see if we need to do this
        className = className.replace('/', '.');
        ValueNumber value = classObjectValueMap.get(className);
        if (value == null) {
            value = createFreshValue(ValueNumber.CONSTANT_CLASS_OBJECT);
            classObjectValueMap.put(className, value);
        }
        return value;
    }

    public @CheckForNull
    @DottedClassName
    String getClassName(ValueNumber v) {
        if (!v.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
            throw new IllegalArgumentException("Not a value number for a constant class");
        }
        for (Map.Entry<String, ValueNumber> e : classObjectValueMap.entrySet()) {
            if (e.getValue().equals(v)) {
                return e.getKey();
            }
        }
        return null;
    }

}

