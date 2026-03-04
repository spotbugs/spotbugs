/*
 * Contributions to SpotBugs
 * Copyright (C) 2026 PANTHEON.tech, s.r.o.
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

import static edu.umd.cs.findbugs.detect.ReflectiveFieldAccessor.*;

import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.detect.ReflectiveInvocation.SpecifiedInvocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final List<ReflectiveInvocation> reflectiveInvocations = new ArrayList<>();

    void newAccessorDeclared(final ReflectiveFieldAccessorBuilder accessorBuilder) {
        XField actualField = accessorBuilder.getActualField();
        XField accessorField = accessorBuilder.getAccessorField();

        ReflectiveFieldAccessLog reflectiveAccessLog = reflectiveFieldAccessMap
                .computeIfAbsent(actualField, k -> new ReflectiveFieldAccessLog(actualField));

        ReflectiveFieldAccessor reflectiveFieldAccessor = accessorBuilder.buildWithAccessLog(reflectiveAccessLog);

        if (reflectiveFieldAccessor instanceof ImplicitAccessor) {
            implicitAccessorMap.putIfAbsent(accessorField, (ImplicitAccessor) reflectiveFieldAccessor);
        } else {
            explicitAccessorMap.putIfAbsent(accessorField, (ExplicitAccessor) reflectiveFieldAccessor);
        }
    }

    void registerReflectiveInvocation(final ReflectiveInvocation invocation) {
        reflectiveInvocations.add(invocation);
    }

    public void resolve() {
        for (ReflectiveInvocation invocation : reflectiveInvocations) {
            XField accessorField = invocation.getAccessorField();
            if (invocation instanceof SpecifiedInvocation) {
                ExplicitAccessor foundAccessor = explicitAccessorMap.get(accessorField);
                if (foundAccessor != null) {
                    foundAccessor.markAccess(((SpecifiedInvocation) invocation).getAccessType());
                }
            } else {
                ImplicitAccessor foundAccessor = implicitAccessorMap.get(accessorField);
                if (foundAccessor != null) {
                    foundAccessor.markAccess();
                }
            }
        }
    }

    public Collection<? extends XField> getWrittenFields(final XFactory xFactory) {
        List<XField> allWrittenFields = new ArrayList<>();
        Collection<XField> allKnownFields = xFactory.allFields();
        for (ReflectiveFieldAccessLog access : reflectiveFieldAccessMap.values()) {
            if (access.wasSetterInvoked()) {
                XField found = findMatchingAmongAll(access.getActualField(), allKnownFields);
                if (found != null) {
                    allWrittenFields.add(found);
                }
            }
        }
        return allWrittenFields;
    }

    public Collection<? extends XField> getReadFields(final XFactory xFactory) {
        List<XField> allReadFields = new ArrayList<>();
        Collection<XField> allKnownFields = xFactory.allFields();
        for (ReflectiveFieldAccessLog access : reflectiveFieldAccessMap.values()) {
            if (access.wasGetterInvoked()) {
                XField found = findMatchingAmongAll(access.getActualField(), allKnownFields);
                if (found != null) {
                    allReadFields.add(found);
                }
            }
        }
        return allReadFields;
    }

    private XField findMatchingAmongAll(final XField matchField, final Collection<XField> allKnownFields) {
        FieldDescriptor fieldDsc = matchField.getFieldDescriptor();
        return allKnownFields.stream()
                .filter(field -> field.getFieldDescriptor().equals(fieldDsc))
                .findFirst()
                .orElse(null);
    }
}
