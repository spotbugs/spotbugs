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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.util.Util;

/**
 * A dataflow value representing a Java stack frame with value number
 * information.
 *
 * @author David Hovemeyer
 * @see ValueNumber
 * @see ValueNumberAnalysis
 */
public class ValueNumberFrame extends Frame<ValueNumber> implements ValueNumberAnalysisFeatures {

    private ArrayList<ValueNumber> mergedValueList;

    private AvailableLoadBiMap availableLoadMap;

    private Map<AvailableLoad, ValueNumber> mergedLoads;

    private Map<ValueNumber, AvailableLoad> previouslyKnownAs;

    public boolean phiNodeForLoads;

    private static final boolean USE_WRITTEN_OUTSIDE_OF_CONSTRUCTOR = true;

    static int constructedUnmodifiableMap;

    static int reusedMap;

    static int createdEmptyMap;

    static int madeImmutableMutable;

    static int reusedMutableMap;

    static {
        Util.runLogAtShutdown(() -> {
            System.err.println("Getting updatable previously known as:");
            System.err.println("  " + createdEmptyMap + " created empty map");
            System.err.println("  " + madeImmutableMutable + " made immutable map mutable");
            System.err.println("  " + reusedMutableMap + " reused mutable map");
            System.err.println("Copying map:");
            System.err.println("  " + constructedUnmodifiableMap + " made mutable map unmodifiable");
            System.err.println("  " + reusedMap + " reused immutable map");
            System.err.println();

        });
    }

    public ValueNumberFrame(int numLocals) {
        super(numLocals);
        if (REDUNDANT_LOAD_ELIMINATION) {
            setAvailableLoadMap(AvailableLoadBiMap.emptyMap());
            setMergedLoads(Collections.<AvailableLoad, ValueNumber>emptyMap());
            setPreviouslyKnownAs(Collections.<ValueNumber, AvailableLoad>emptyMap());
        }
    }

    public String availableLoadMapAsString() {
        StringBuilder buf = new StringBuilder("{ ");
        for (Map.Entry<AvailableLoad, ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
            buf.append(e.getKey());
            buf.append("=");
            for (ValueNumber v : e.getValue()) {
                buf.append(v).append(",");
            }
            buf.append(";  ");
        }

        buf.append(" }");
        return buf.toString();
    }

    public @CheckForNull AvailableLoad getLoad(ValueNumber v) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return null;
        }

        return getAvailableLoadMap().getLoad(v);
    }

    /**
     * Look for an available load.
     *
     * @param availableLoad
     *            the AvailableLoad (reference and field)
     * @return the value(s) available, or null if no matching entry is found
     */
    public ValueNumber[] getAvailableLoad(AvailableLoad availableLoad) {
        return getAvailableLoadMap().get(availableLoad);
    }

    /**
     * Add an available load.
     *
     * @param availableLoad
     *            the AvailableLoad (reference and field)
     * @param value
     *            the value(s) loaded
     */
    public void addAvailableLoad(AvailableLoad availableLoad, @Nonnull ValueNumber[] value) {
        Objects.requireNonNull(value);
        getUpdateableAvailableLoadMap().put(availableLoad, value);

        for (ValueNumber v : value) {
            getUpdateablePreviouslyKnownAs().put(v, availableLoad);
            if (RLE_DEBUG) {
                System.out.println("Adding available load of " + availableLoad + " for " + v + " to "
                        + System.identityHashCode(this));
            }
        }
    }

    private static void removeAllKeys(AvailableLoadBiMap map, Iterable<AvailableLoad> removeMe) {
        for (AvailableLoad k : removeMe) {
            map.remove(k);
        }
    }

    /**
     * Kill all loads of given field.
     *
     * @param field
     *            the field
     */
    public void killLoadsOfField(XField field) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {
            if (availableLoad.getField().equals(field)) {
                if (RLE_DEBUG) {
                    System.out.println("KILLING Load of " + availableLoad + " in " + this);
                }
                killMe.add(availableLoad);
            }
        }
        killAvailableLoads(killMe);
    }

    /**
     * Kill all loads. This conservatively handles method calls where we don't
     * really know what fields might be assigned.
     */
    public void killAllLoads() {
        killAllLoads(false);
    }

    public void killAllLoads(boolean primitiveOnly) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {
            XField field = availableLoad.getField();
            if ((!primitiveOnly || !field.isReferenceType()) && (field.isVolatile() || !field.isFinal()
                    && (!USE_WRITTEN_OUTSIDE_OF_CONSTRUCTOR || fieldSummary.isWrittenOutsideOfConstructor(field)))) {
                if (RLE_DEBUG) {
                    System.out.println("KILLING load of " + availableLoad + " in " + this);
                }
                killMe.add(availableLoad);
            }
        }
        killAvailableLoads(killMe);

    }

    public void killAllLoadsExceptFor(@CheckForNull ValueNumber v) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        AvailableLoad myLoad = getLoad(v);
        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {
            if (!availableLoad.getField().isFinal() && !availableLoad.equals(myLoad)) {
                if (RLE_DEBUG) {
                    System.out.println("KILLING load of " + availableLoad + " in " + this);
                }
                killMe.add(availableLoad);
            }
        }
        killAvailableLoads(killMe);
    }

    /**
     * Kill all loads. This conservatively handles method calls where we don't
     * really know what fields might be assigned.
     */
    public void killAllLoadsOf(@CheckForNull ValueNumber v) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {
            if (availableLoad.getReference() != v) {
                continue;
            }
            XField field = availableLoad.getField();
            if (!field.isFinal() && (!USE_WRITTEN_OUTSIDE_OF_CONSTRUCTOR || fieldSummary.isWrittenOutsideOfConstructor(field))) {
                if (RLE_DEBUG) {
                    System.out.println("Killing load of " + availableLoad + " in " + this);
                }
                killMe.add(availableLoad);
            }
        }
        killAvailableLoads(killMe);
    }

    public void killLoadsOf(Set<XField> fieldsToKill) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {

            if (fieldsToKill.contains(availableLoad.getField())) {
                killMe.add(availableLoad);
            }

        }
        killAvailableLoads(killMe);
    }

    public void killLoadsWithSimilarName(String className, String methodName) {
        if (!REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        String packageName = extractPackageName(className);

        HashSet<AvailableLoad> killMe = new HashSet<>();
        for (AvailableLoad availableLoad : getAvailableLoadMap().keySet()) {

            XField field = availableLoad.getField();
            String fieldPackageName = extractPackageName(field.getClassName());
            if (packageName.equals(fieldPackageName) && field.isStatic()
                    && methodName.toLowerCase().indexOf(field.getName().toLowerCase()) >= 0) {
                killMe.add(availableLoad);
            }

        }
        killAvailableLoads(killMe);
    }

    private void killAvailableLoads(HashSet<AvailableLoad> killMe) {
        if (killMe.size() > 0) {
            removeAllKeys(getUpdateableAvailableLoadMap(), killMe);
        }
    }

    private String extractPackageName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    void mergeAvailableLoadSets(ValueNumberFrame other, ValueNumberFactory factory, MergeTree mergeTree) {
        if (REDUNDANT_LOAD_ELIMINATION) {
            // Merge available load sets.
            // Only loads that are available in both frames
            // remain available. All others are discarded.
            String s = "";
            if (RLE_DEBUG) {
                s = "Merging " + this.availableLoadMapAsString() + " and " + other.availableLoadMapAsString();
            }
            boolean changed = false;
            if (other.isBottom()) {
                changed = !this.getAvailableLoadMap().isEmpty();
                setAvailableLoadMap(AvailableLoadBiMap.emptyMap());
            } else if (!other.isTop()) {
                AvailableLoadBiMap updateableAvailableLoadMap = getUpdateableAvailableLoadMap();

                for (Map.Entry<AvailableLoad, ValueNumber[]> e : updateableAvailableLoadMap.entrySet()) {
                    AvailableLoad load = e.getKey();
                    ValueNumber[] myVN = e.getValue();
                    ValueNumber[] otherVN = other.getAvailableLoadMap().get(load);
                    /*
                    if (false && this.phiNodeForLoads && myVN != null && myVN.length == 1
                            && myVN[0].hasFlag(ValueNumber.PHI_NODE)) {
                        continue;
                    }
                     */
                    if (!Arrays.equals(myVN, otherVN)) {

                        ValueNumber phi = getMergedLoads().get(load);
                        if (phi == null) {
                            int flags = -1;
                            for (ValueNumber vn : myVN) {
                                flags = ValueNumber.mergeFlags(flags, vn.getFlags());
                            }
                            if (otherVN != null) {
                                for (ValueNumber vn : otherVN) {
                                    flags = ValueNumber.mergeFlags(flags, vn.getFlags());
                                }
                            }
                            if (flags == -1) {
                                flags = ValueNumber.PHI_NODE;
                            } else {
                                flags |= ValueNumber.PHI_NODE;
                            }

                            phi = factory.createFreshValue(flags);

                            getUpdateableMergedLoads().put(load, phi);
                            for (ValueNumber vn : myVN) {
                                mergeTree.mapInputToOutput(vn, phi);
                            }
                            if (otherVN != null) {
                                for (ValueNumber vn : otherVN) {
                                    mergeTree.mapInputToOutput(vn, phi);
                                }
                            }

                            if (RLE_DEBUG) {
                                System.out.println("Creating phi node " + phi + " for " + load + " from " + Arrays.toString(myVN)
                                        + " x " + Arrays.toString(otherVN) + " in " + System.identityHashCode(this));
                            }
                            changed = true;
                            updateableAvailableLoadMap.updateEntryValue(e, phi);
                        } else {
                            if (RLE_DEBUG) {
                                System.out.println("Reusing phi node : " + phi + " for " + load + " from "
                                        + Arrays.toString(myVN) + " x " + Arrays.toString(otherVN) + " in "
                                        + System.identityHashCode(this));
                            }
                            if (myVN.length != 1 || !myVN[0].equals(phi)) {
                                updateableAvailableLoadMap.updateEntryValue(e, phi);
                            }
                        }

                    }

                }
            }
            Map<ValueNumber, AvailableLoad> previouslyKnownAsOther = other.getPreviouslyKnownAs();
            if (getPreviouslyKnownAs() != previouslyKnownAsOther && previouslyKnownAsOther.size() != 0) {
                if (getPreviouslyKnownAs().size() == 0) {
                    assignPreviouslyKnownAs(other);
                } else {
                    getUpdateablePreviouslyKnownAs().putAll(previouslyKnownAsOther);
                }
            }
            if (changed) {
                this.phiNodeForLoads = true;
            }
            if (changed && RLE_DEBUG) {
                System.out.println(s);
                System.out.println("  Result is " + this.availableLoadMapAsString());
                System.out.println(" Set phi for " + System.identityHashCode(this));
            }
        }
    }

    ValueNumber getMergedValue(int slot) {
        return mergedValueList.get(slot);
    }

    void setMergedValue(int slot, ValueNumber value) {
        mergedValueList.set(slot, value);
    }

    @Override
    public void copyFrom(Frame<ValueNumber> other) {
        if (!(other instanceof ValueNumberFrame)) {
            throw new IllegalArgumentException();
        }
        // If merged value list hasn't been created yet, create it.
        if (mergedValueList == null && other.isValid()) {
            // This is where this frame gets its size.
            // It will have the same size as long as it remains valid.
            mergedValueList = new ArrayList<>(other.getNumSlots());
            int numSlots = other.getNumSlots();
            for (int i = 0; i < numSlots; ++i) {
                mergedValueList.add(null);
            }
        }

        if (REDUNDANT_LOAD_ELIMINATION) {
            assignAvailableLoadMap((ValueNumberFrame) other);
            assignPreviouslyKnownAs((ValueNumberFrame) other);
        }

        super.copyFrom(other);
    }

    private void assignAvailableLoadMap(ValueNumberFrame other) {
        AvailableLoadBiMap availableLoadMapOther = other.getAvailableLoadMap();
        if (availableLoadMapOther.isModifiable()) {
            availableLoadMapOther = AvailableLoadBiMap.unmodifiableMap(availableLoadMapOther);
            other.setAvailableLoadMap(availableLoadMapOther);
            setAvailableLoadMap(availableLoadMapOther);
            constructedUnmodifiableMap++;
        } else {
            setAvailableLoadMap(availableLoadMapOther);
            reusedMap++;
        }
    }

    private void assignPreviouslyKnownAs(ValueNumberFrame other) {
        Map<ValueNumber, AvailableLoad> previouslyKnownAsOther = other.getPreviouslyKnownAs();
        if (previouslyKnownAsOther instanceof HashMap) {
            previouslyKnownAsOther = Collections.<ValueNumber, AvailableLoad>unmodifiableMap(previouslyKnownAsOther);
            other.setPreviouslyKnownAs(previouslyKnownAsOther);
            setPreviouslyKnownAs(previouslyKnownAsOther);
            constructedUnmodifiableMap++;
        } else {
            setPreviouslyKnownAs(previouslyKnownAsOther);
            reusedMap++;
        }
    }

    @Override
    public String toString() {
        String frameValues = super.toString();
        if (RLE_DEBUG) {
            StringBuilder buf = new StringBuilder();
            buf.append(frameValues);

            Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator();
            boolean first = true;
            while (i.hasNext()) {
                AvailableLoad key = i.next();
                ValueNumber[] value = getAvailableLoadMap().get(key);
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(key + "=" + valueToString(value));
            }

            buf.append(" #");
            buf.append(System.identityHashCode(this));
            if (phiNodeForLoads) {
                buf.append(" phi");
            }
            return buf.toString();
        } else {
            return frameValues;
        }
    }

    private static String valueToString(ValueNumber[] valueNumberList) {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        boolean first = true;
        for (ValueNumber aValueNumberList : valueNumberList) {
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(aValueNumberList.getNumber());
        }
        buf.append(']');
        return buf.toString();
    }

    public boolean fuzzyMatch(ValueNumber v1, ValueNumber v2) {
        if (REDUNDANT_LOAD_ELIMINATION) {
            return v1.equals(v2) || fromMatchingLoads(v1, v2) || haveMatchingFlags(v1, v2);
        } else {
            return v1.equals(v2);
        }
    }

    public boolean veryFuzzyMatch(ValueNumber v1, ValueNumber v2) {
        if (REDUNDANT_LOAD_ELIMINATION) {
            return v1.equals(v2) || fromMatchingFields(v1, v2) || haveMatchingFlags(v1, v2);
        } else {
            return v1.equals(v2);
        }
    }

    public boolean fromMatchingLoads(ValueNumber v1, ValueNumber v2) {
        AvailableLoad load1 = getLoad(v1);
        if (load1 == null) {
            load1 = getPreviouslyKnownAs().get(v1);
        }
        if (load1 == null) {
            return false;
        }
        AvailableLoad load2 = getLoad(v2);
        if (load2 == null) {
            load2 = getPreviouslyKnownAs().get(v2);
        }
        if (load2 == null) {
            return false;
        }
        return load1.equals(load2);
    }

    public boolean fromMatchingFields(ValueNumber v1, ValueNumber v2) {
        AvailableLoad load1 = getLoad(v1);
        if (load1 == null) {
            load1 = getPreviouslyKnownAs().get(v1);
        }
        if (load1 == null) {
            return false;
        }
        AvailableLoad load2 = getLoad(v2);
        if (load2 == null) {
            load2 = getPreviouslyKnownAs().get(v2);
        }
        if (load2 == null) {
            return false;
        }
        if (load1.equals(load2)) {
            return true;
        }
        if (load1.getField().equals(load2.getField())) {
            ValueNumber source1 = load1.getReference();
            ValueNumber source2 = load2.getReference();
            if (!this.contains(source1)) {
                return true;
            }
            if (!this.contains(source2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if v1 and v2 have a flag in common
     */
    public boolean haveMatchingFlags(ValueNumber v1, ValueNumber v2) {
        int flag1 = v1.getFlags();
        int flag2 = v2.getFlags();
        return (flag1 & flag2) != 0;
    }

    public Collection<ValueNumber> valueNumbersForLoads() {
        HashSet<ValueNumber> result = new HashSet<>();
        if (REDUNDANT_LOAD_ELIMINATION) {
            for (Map.Entry<AvailableLoad, ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
                if (e.getValue() != null) {
                    Collections.addAll(result, e.getValue());
                }
            }
        }

        return result;
    }

    private void setAvailableLoadMap(AvailableLoadBiMap availableLoadMap) {
        this.availableLoadMap = availableLoadMap;
    }

    private AvailableLoadBiMap getAvailableLoadMap() {
        return availableLoadMap;
    }

    private AvailableLoadBiMap getUpdateableAvailableLoadMap() {
        if (!availableLoadMap.isModifiable()) {
            HashMap<AvailableLoad, ValueNumber[]> tmp = new HashMap<>(availableLoadMap.size() + 4);
            tmp.putAll(availableLoadMap.map);
            availableLoadMap = new AvailableLoadBiMap(tmp);
        }
        return availableLoadMap;
    }

    private void setMergedLoads(Map<AvailableLoad, ValueNumber> mergedLoads) {
        this.mergedLoads = mergedLoads;
    }

    private Map<AvailableLoad, ValueNumber> getMergedLoads() {
        return mergedLoads;
    }

    private Map<AvailableLoad, ValueNumber> getUpdateableMergedLoads() {
        if (!(mergedLoads instanceof HashMap)) {
            mergedLoads = new HashMap<>();
        }

        return mergedLoads;
    }

    private void setPreviouslyKnownAs(Map<ValueNumber, AvailableLoad> previouslyKnownAs) {
        this.previouslyKnownAs = previouslyKnownAs;
    }

    private Map<ValueNumber, AvailableLoad> getPreviouslyKnownAs() {
        return previouslyKnownAs;
    }

    private Map<ValueNumber, AvailableLoad> getUpdateablePreviouslyKnownAs() {
        if (previouslyKnownAs.size() == 0) {
            previouslyKnownAs = new HashMap<>(4);
            createdEmptyMap++;
        } else if (!(previouslyKnownAs instanceof HashMap)) {
            HashMap<ValueNumber, AvailableLoad> tmp = new HashMap<>(previouslyKnownAs.size() + 4);
            tmp.putAll(previouslyKnownAs);
            previouslyKnownAs = tmp;
            madeImmutableMutable++;
        } else {
            reusedMutableMap++;
        }

        return previouslyKnownAs;
    }

    @Override
    public boolean sameAs(Frame<ValueNumber> other) {
        if (!super.sameAs(other)) {
            return false;
        }
        if (isTop() && other.isTop() || isBottom() && other.isBottom()) {
            return true;
        }
        ValueNumberFrame o = (ValueNumberFrame) other;
        if (availableLoadMap.size() != o.availableLoadMap.size()) {
            return false;
        }
        for (Entry<AvailableLoad, ValueNumber[]> entry : availableLoadMap.entrySet()) {
            ValueNumber[] oValue = o.availableLoadMap.get(entry.getKey());
            if (!Arrays.equals(entry.getValue(), oValue)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAvailableLoads() {
        return !getAvailableLoadMap().isEmpty();
    }

    /**
     * A wrapper for the AvailableLoad to ValueNumber[] map also keeping track of a reverse map. There are a lot of
     * calls to {@link ValueNumberFrame#getLoad(ValueNumber)} so it is faster using a reverse map compared to doing a
     * linear search
     */
    private static class AvailableLoadBiMap {
        private final Map<AvailableLoad, ValueNumber[]> map;
        private final Map<ValueNumber, AvailableLoad> reverseMap;

        public AvailableLoadBiMap(Map<AvailableLoad, ValueNumber[]> map) {
            this.map = map;
            this.reverseMap = new HashMap<>();

            for (Map.Entry<AvailableLoad, ValueNumber[]> entry : map.entrySet()) {
                ValueNumber[] value = entry.getValue();

                for (ValueNumber element : value) {
                    reverseMap.put(element, entry.getKey());
                }
            }
        }

        public AvailableLoadBiMap(Map<AvailableLoad, ValueNumber[]> map, Map<ValueNumber, AvailableLoad> reverseMap) {
            this.map = map;
            this.reverseMap = reverseMap;
        }

        /**
         * @return an empty (unmodifiable) {@link AvailableLoadBiMap}
         */
        public static AvailableLoadBiMap emptyMap() {
            return new AvailableLoadBiMap(Collections.emptyMap(), Collections.emptyMap());
        }

        /**
         * @param other
         *            The map we want to copy
         * @return an unmodifiable copy backed by the <code>other</code> {@link AvailableLoadBiMap}
         */
        public static AvailableLoadBiMap unmodifiableMap(AvailableLoadBiMap other) {
            return new AvailableLoadBiMap(Collections.unmodifiableMap(other.map),
                    Collections.unmodifiableMap(other.reverseMap));
        }

        /**
         * @return The number of distinct {@link AvailableLoad} in this map
         */
        public int size() {
            return map.size();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public Set<AvailableLoad> keySet() {
            return map.keySet();
        }

        public Set<Entry<AvailableLoad, ValueNumber[]>> entrySet() {
            return map.entrySet();
        }

        public ValueNumber[] get(AvailableLoad key) {
            return map.get(key);
        }

        /**
         * Put an array of {@link ValueNumber} for an {@link AvailableLoad} and update the reverse map
         */
        public ValueNumber[] put(AvailableLoad key, ValueNumber[] value) {
            ValueNumber[] previous = map.put(key, value);

            for (ValueNumber v : value) {
                reverseMap.put(v, key);
            }

            return previous;
        }

        public void updateEntryValue(Entry<AvailableLoad, ValueNumber[]> e, ValueNumber value) {
            for (ValueNumber v : e.getValue()) {
                reverseMap.remove(v);
            }

            e.setValue(new ValueNumber[] { value });

            reverseMap.put(value, e.getKey());
        }

        /**
         * Remove an {@link AvailableLoad} and update the reverse map
         */
        public ValueNumber[] remove(AvailableLoad key) {
            ValueNumber[] value = map.remove(key);

            for (ValueNumber v : value) {
                reverseMap.remove(v);
            }

            return value;
        }

        public AvailableLoad getLoad(ValueNumber v) {
            if (!REDUNDANT_LOAD_ELIMINATION) {
                return null;
            }

            return reverseMap.get(v);
        }

        public boolean isModifiable() {
            return map instanceof HashMap;
        }
    }
}
