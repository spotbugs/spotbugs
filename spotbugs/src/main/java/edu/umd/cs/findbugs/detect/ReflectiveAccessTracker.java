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
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
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

    private XFactory xFactory;

    void newAccessorDeclared(final ReflectiveFieldAccessor accessor) {
        accessors.add(accessor);
        accessorByAccessorField.putIfAbsent(accessor.accessorField(), accessor);
        accessorsByActualField.add(accessor.actualField(), accessor);
    }

    void registerReflectiveInvocation(final ReflectiveInvocation invocation) {
        invocations.add(invocation);
    }

    void resolve() {
        xFactory = AnalysisContext.currentXFactory();
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

    List<XField> getAllAccessedFields() {
        Collection<XField> allKnownFields = xFactory.allFields();
        Set<XField> accessedFields = new HashSet<>(gettersInvoked);
        accessedFields.addAll(settersInvoked);
        List<XField> result = new ArrayList<>();
        for (XField accessedField : accessedFields) {
            XField knownField = findMatchingAmongAll(accessedField, allKnownFields);
            if (knownField != null) {
                result.add(knownField);
            }
        }
        return result;
    }

    private Map<XField, SourceLineAnnotation> selectFieldsWithLines(final Predicate<XField> filter) {
        Collection<XField> allKnownFields = xFactory.allFields();
        Map<XField, SourceLineAnnotation> fieldToLineMap = new LinkedHashMap<>();
        for (XField actualField : accessorsByActualField.keySet()) {
            if (!filter.test(actualField)) {
                continue;
            }
            XField knownField = findMatchingAmongAll(actualField, allKnownFields);
            if (knownField != null) {
                fieldToLineMap.put(knownField, getMostRelevantAccessorLine(actualField));
            }
        }
        return fieldToLineMap;
    }

    MultiMap<XField, SourceLineAnnotation> getUnusedAccessorDeclarationLines() {
        Collection<XField> allKnownFields = xFactory.allFields();
        MultiMap<XField, SourceLineAnnotation> fieldToLinesMap = new MultiMap<>(ArrayList.class);
        for (ReflectiveFieldAccessor accessor : accessors) {
            if (accessor.wasUsed()) {
                continue;
            }
            XField knownField = findMatchingAmongAll(accessor.actualField(), allKnownFields);
            if (knownField != null) {
                fieldToLinesMap.add(knownField, accessor.sourceLine());
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

    private XField findMatchingAmongAll(final XField matchField, final Collection<XField> allKnownFields) {
        FieldDescriptor fieldDsc = matchField.getFieldDescriptor();
        for (XField field : allKnownFields) {
            if (field.getFieldDescriptor().equals(fieldDsc)) {
                return field;
            }
        }
        return null;
    }
}
