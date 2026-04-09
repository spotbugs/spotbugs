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
import edu.umd.cs.findbugs.detect.ReflectiveFieldAccessor.ExplicitAccessor;
import edu.umd.cs.findbugs.detect.ReflectiveFieldAccessor.ImplicitAccessor;
import edu.umd.cs.findbugs.detect.ReflectiveFieldAccessorInvocation.TypeAwareRFAInvocation;
import edu.umd.cs.findbugs.util.MultiMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tracks the fields accessed through reflection such as VarHandles, MethodHandles and AtomicFieldUpdaters.
 */
class ReflectiveAccessTracker {

    enum AccessType {
        GETTER,
        SETTER,
        BOTH
    }

    private final Map<XField, ReflectiveFieldAccessLog> reflectiveFieldAccessMap = new HashMap<>();
    private final Map<XField, ExplicitAccessor> explicitAccessorMap = new HashMap<>();
    private final Map<XField, ImplicitAccessor> implicitAccessorMap = new HashMap<>();
    private final List<ReflectiveFieldAccessorInvocation> reflectiveInvocations = new ArrayList<>();

    // convenience map to simplify lookup of unused accessors
    private final MultiMap<ReflectiveFieldAccessLog, ReflectiveFieldAccessor> mapFieldToAccessors = new MultiMap<>(HashSet.class);

    private XFactory xFactory;

    void newAccessorDeclared(final ReflectiveFieldAccessorBuilder accessorBuilder) {
        XField actualField = accessorBuilder.getActualField();
        XField accessorField = accessorBuilder.getAccessorField();

        ReflectiveFieldAccessLog reflectiveAccessLog = reflectiveFieldAccessMap
                .computeIfAbsent(actualField, k -> new ReflectiveFieldAccessLog(actualField));

        ReflectiveFieldAccessor reflectiveFieldAccessor = accessorBuilder.buildWithAccessLog(reflectiveAccessLog);
        mapFieldToAccessors.add(reflectiveAccessLog, reflectiveFieldAccessor);

        if (reflectiveFieldAccessor instanceof ImplicitAccessor) {
            implicitAccessorMap.putIfAbsent(accessorField, (ImplicitAccessor) reflectiveFieldAccessor);
        } else {
            explicitAccessorMap.putIfAbsent(accessorField, (ExplicitAccessor) reflectiveFieldAccessor);
        }
    }

    void registerReflectiveInvocation(final ReflectiveFieldAccessorInvocation invocation) {
        reflectiveInvocations.add(invocation);
    }

    void resolve() {
        xFactory = AnalysisContext.currentXFactory();
        for (ReflectiveFieldAccessorInvocation invocation : reflectiveInvocations) {
            XField accessorField = invocation.getAccessorField();
            if (invocation instanceof TypeAwareRFAInvocation) {
                ExplicitAccessor foundAccessor = explicitAccessorMap.get(accessorField);
                if (foundAccessor != null) {
                    foundAccessor.markAccess(((TypeAwareRFAInvocation) invocation).getAccessType());
                }
            } else {
                ImplicitAccessor foundAccessor = implicitAccessorMap.get(accessorField);
                if (foundAccessor != null) {
                    foundAccessor.markAccess();
                }
            }
        }
    }

    Map<XField, SourceLineAnnotation> getFieldsNeverWritten() {
        return selectFieldsWithLines(log -> log.wasGetterInvoked() && !log.wasSetterInvoked());
    }

    Map<XField, SourceLineAnnotation> getFieldsNeverRead() {
        return selectFieldsWithLines(log -> log.wasSetterInvoked() && !log.wasGetterInvoked());
    }

    List<? extends XField> getAllAccessedFields() {
        return selectFields(log -> log.wasGetterInvoked() || log.wasSetterInvoked());
    }

    private List<XField> selectFields(final Predicate<ReflectiveFieldAccessLog> accessFilter) {
        Collection<XField> allKnownFields = xFactory.allFields();
        return reflectiveFieldAccessMap.values().stream()
                .filter(accessFilter)
                .map(access -> findMatchingAmongAll(access.getActualField(), allKnownFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<XField, SourceLineAnnotation> selectFieldsWithLines(final Predicate<ReflectiveFieldAccessLog> accessFilter) {
        Collection<XField> allKnownFields = xFactory.allFields();
        Map<XField, SourceLineAnnotation> fieldToLineMap = new LinkedHashMap<>();
        reflectiveFieldAccessMap.values().stream()
                .filter(accessFilter)
                .forEach(log -> {
                    XField knownField = findMatchingAmongAll(log.getActualField(), allKnownFields);
                    if (knownField != null) {
                        fieldToLineMap.put(knownField, getMostRelevantAccessorLine(log));
                    }
                });
        return fieldToLineMap;
    }

    MultiMap<XField, ReflectiveFieldAccessor> getUnusedAccessorsByActualField() {
        Collection<XField> allKnownFields = xFactory.allFields();
        MultiMap<XField, ReflectiveFieldAccessor> fieldToAccessorsMap = new MultiMap<>(ArrayList.class);
        getUnusedAccessors().forEach(accessor -> {
            XField knownField = findMatchingAmongAll(accessor.getReflectiveAccessLog().getActualField(), allKnownFields);
            if (knownField != null) {
                fieldToAccessorsMap.add(knownField, accessor);
            }
        });
        return fieldToAccessorsMap;
    }

    private List<ReflectiveFieldAccessor> getUnusedAccessors() {
        return mapFieldToAccessors.asMap().values().stream()
                .flatMap(Collection::stream)
                .filter(a -> !a.wasUsed())
                .collect(Collectors.toList());
    }

    /**
     * Returns the declaration source line of the most relevant accessor for a bug report.
     * When the field is read but never written, points to the setter-accessor's declaration line.
     * When the field is written but never read, points to the getter-accessor's declaration line.
     * Otherwise falls back to the declaration line of the first registered accessor.
     */
    private SourceLineAnnotation getMostRelevantAccessorLine(final ReflectiveFieldAccessLog log) {
        Collection<ReflectiveFieldAccessor> accessors = mapFieldToAccessors.get(log);
        Optional<SourceLineAnnotation> foundAnnotation = Optional.empty();
        if (log.wasGetterInvoked() && !log.wasSetterInvoked()) {
            foundAnnotation = findAccessorLineFor(AccessType.SETTER, accessors);
        } else if (log.wasSetterInvoked() && !log.wasGetterInvoked()) {
            foundAnnotation = findAccessorLineFor(AccessType.GETTER, accessors);
        }
        return foundAnnotation.orElse(firstAccessorLine(accessors));
    }

    private SourceLineAnnotation firstAccessorLine(final Collection<ReflectiveFieldAccessor> accessors) {
        return accessors.stream()
                .map(ReflectiveFieldAccessor::getDeclarationSourceLine)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Optional<SourceLineAnnotation> findAccessorLineFor(final AccessType accessType,
            final Collection<ReflectiveFieldAccessor> accessors) {
        return accessors.stream()
                .filter(accessor -> accessor.getAccessType() == accessType || accessor.getAccessType() == AccessType.BOTH)
                .map(ReflectiveFieldAccessor::getDeclarationSourceLine)
                .findFirst();
    }

    private XField findMatchingAmongAll(final XField matchField, final Collection<XField> allKnownFields) {
        FieldDescriptor fieldDsc = matchField.getFieldDescriptor();
        return allKnownFields.stream()
                .filter(field -> field.getFieldDescriptor().equals(fieldDsc))
                .findFirst()
                .orElse(null);
    }
}
