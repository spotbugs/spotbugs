/*
 * SpotBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.util.MultiMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tracks the fields accessed through reflection such as VarHandles, MethodHandles and AtomicFieldUpdaters.
 * <p>
 * Only one shape is recognized, and every condition below has to hold for a field to be tracked:
 * <ul>
 * <li>the accessor is created by {@code Lookup.findVarHandle}, {@code Lookup.findGetter},
 * {@code Lookup.findSetter}, or {@code newUpdater} on one of the reference, integer and long
 * {@code AtomicFieldUpdater} classes. All of those target a non-static field, so the accessed field
 * is always an instance field.</li>
 * <li>the accessor is stored into a <em>static</em> field by the instruction immediately following
 * the creating call.</li>
 * <li>the accessor is invoked directly on the value loaded from that static field.</li>
 * <li>that static field is assigned exactly once.</li>
 * </ul>
 * <p>
 * Everything else is invisible here, so such a field keeps whatever {@code UUF}/{@code URF}/
 * {@code UWF} report the ordinary analysis produces for it. That includes accessors stored into an
 * instance field, kept in a local variable, cached in an array or a map, returned from a factory
 * method or never stored at all; accessors whose accessed class or field name is not a compile-time
 * constant at the creation site; static target fields via {@code findStaticGetter},
 * {@code findStaticSetter} or {@code findStaticVarHandle}; accessors obtained from
 * {@code Lookup.unreflect*}, {@code java.lang.reflect.Field} or {@code Unsafe}; and invocations made
 * on a derived accessor such as the result of {@code MethodHandle.asType}, {@code bindTo} or
 * {@code VarHandle.toMethodHandle}.
 */
class ReflectiveAccessTracker {

    enum AccessType {
        GETTER,
        SETTER,
        BOTH
    }

    private final List<ReflectiveFieldAccessor> accessors = new ArrayList<>();
    private final Map<XField, ReflectiveFieldAccessor> accessorByAccessorField = new HashMap<>();
    private final MultiMap<XField, ReflectiveFieldAccessor> accessorsByActualField = new MultiMap<>(ArrayList.class);
    private final List<ReflectiveInvocation> invocations = new ArrayList<>();

    // actual fields read / written through their accessors, populated during resolve().
    private final Set<XField> gettersInvoked = new HashSet<>();
    private final Set<XField> settersInvoked = new HashSet<>();

    void newAccessorDeclared(final ReflectiveFieldAccessor accessor) {
        accessors.add(accessor);
        accessorByAccessorField.putIfAbsent(accessor.accessorField(), accessor);
        accessorsByActualField.add(accessor.actualField(), accessor);
    }

    void registerReflectiveInvocation(final ReflectiveInvocation invocation) {
        invocations.add(invocation);
    }

    void resolve() {
        for (ReflectiveInvocation invocation : invocations) {
            ReflectiveFieldAccessor accessor = accessorByAccessorField.get(invocation.accessorField());
            if (accessor == null) {
                continue;
            }
            accessor.markUsed();
            // A MethodHandle invocation carries no type; fall back to the accessor's declared type.
            AccessType accessType = invocation.accessType() != null ? invocation.accessType() : accessor.accessType();
            if (accessType == AccessType.GETTER || accessType == AccessType.BOTH) {
                gettersInvoked.add(accessor.actualField());
            }
            if (accessType == AccessType.SETTER || accessType == AccessType.BOTH) {
                settersInvoked.add(accessor.actualField());
            }
        }
    }

    Map<XField, SourceLineAnnotation> getFieldsNeverWritten() {
        return selectFieldsWithLines(field -> gettersInvoked.contains(field) && !settersInvoked.contains(field));
    }

    Map<XField, SourceLineAnnotation> getFieldsNeverRead() {
        return selectFieldsWithLines(field -> settersInvoked.contains(field) && !gettersInvoked.contains(field));
    }

    Set<XField> getAllAccessedFields() {
        Set<XField> accessedFields = new HashSet<>(gettersInvoked);
        accessedFields.addAll(settersInvoked);
        return accessedFields;
    }

    private Map<XField, SourceLineAnnotation> selectFieldsWithLines(final Predicate<XField> filter) {
        Map<XField, SourceLineAnnotation> fieldToLineMap = new LinkedHashMap<>();
        for (XField actualField : accessorsByActualField.keySet()) {
            if (filter.test(actualField)) {
                fieldToLineMap.put(actualField, getMostRelevantAccessorLine(actualField));
            }
        }
        return fieldToLineMap;
    }

    MultiMap<XField, SourceLineAnnotation> getUnusedAccessorDeclarationLines() {
        MultiMap<XField, SourceLineAnnotation> fieldToLinesMap = new MultiMap<>(ArrayList.class);
        for (ReflectiveFieldAccessor accessor : accessors) {
            if (!accessor.wasUsed()) {
                fieldToLinesMap.add(accessor.actualField(), accessor.sourceLine());
            }
        }
        return fieldToLinesMap;
    }

    /**
     * Returns the declaration source line of the most relevant accessor for a bug report.
     * When the field is read but never written, points to the setter-accessor's declaration line.
     * When the field is written but never read, points to the getter-accessor's declaration line.
     * Otherwise falls back to the declaration line of the first registered accessor.
     */
    private SourceLineAnnotation getMostRelevantAccessorLine(final XField actualField) {
        Collection<ReflectiveFieldAccessor> fieldAccessors = accessorsByActualField.get(actualField);
        SourceLineAnnotation foundLine = null;
        if (gettersInvoked.contains(actualField) && !settersInvoked.contains(actualField)) {
            foundLine = findAccessorLineFor(AccessType.SETTER, fieldAccessors);
        } else if (settersInvoked.contains(actualField) && !gettersInvoked.contains(actualField)) {
            foundLine = findAccessorLineFor(AccessType.GETTER, fieldAccessors);
        }
        return foundLine != null ? foundLine : firstAccessorLine(fieldAccessors);
    }

    private SourceLineAnnotation firstAccessorLine(final Collection<ReflectiveFieldAccessor> fieldAccessors) {
        for (ReflectiveFieldAccessor accessor : fieldAccessors) {
            if (accessor.sourceLine() != null) {
                return accessor.sourceLine();
            }
        }
        return null;
    }

    private SourceLineAnnotation findAccessorLineFor(final AccessType accessType,
            final Collection<ReflectiveFieldAccessor> fieldAccessors) {
        for (ReflectiveFieldAccessor accessor : fieldAccessors) {
            if (accessor.accessType() == accessType || accessor.accessType() == AccessType.BOTH) {
                return accessor.sourceLine();
            }
        }
        return null;
    }
}
