/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * @author  pugh
 */
public class UnreadFieldsData {
    final Map<XField, Set<ProgramPoint>> assumedNonNull = new HashMap<>();

    final Map<XField, ProgramPoint> threadLocalAssignedInConstructor = new HashMap<>();

    final Set<XField> nullTested = new HashSet<>();

    final Set<XField> containerFields = new TreeSet<>();

    final MultiMap<XField, String> unknownAnnotation = new MultiMap<>(LinkedList.class);

    final Set<String> abstractClasses = new HashSet<>();

    final Set<String> hasNonAbstractSubClass = new HashSet<>();

    final Set<String> classesScanned = new HashSet<>();

    final Set<XField> fieldsOfNativeClasses = new HashSet<>();

    final Set<XField> reflectiveFields = new HashSet<>();

    final Set<XField> fieldsOfSerializableOrNativeClassed = new HashSet<>();

    final Set<XField> staticFieldsReadInThisMethod = new HashSet<>();

    final Set<XField> allMyFields = new TreeSet<>();

    final Set<XField> myFields = new TreeSet<>();

    final Set<XField> writtenFields = new HashSet<>();


    final Map<XField, SourceLineAnnotation> fieldAccess = new HashMap<>();

    final Set<XField> writtenNonNullFields = new HashSet<>();

    final Set<String> calledFromConstructors = new HashSet<>();

    final Set<XField> writtenInConstructorFields = new HashSet<>();

    final Set<XField> writtenInInitializationFields = new HashSet<>();

    final Set<XField> writtenOutsideOfInitializationFields = new HashSet<>();

    final Set<XField> readFields = new HashSet<>();

    final Set<XField> constantFields = new HashSet<>();

    final Set<String> needsOuterObjectInConstructor = new HashSet<>();

    final Set<String> innerClassCannotBeStatic = new HashSet<>();

    final HashSet<ClassDescriptor> toldStrongEvidenceForIntendedSerialization = new HashSet<>();

    public boolean isContainerField(XField f) {
        return containerFields.contains(f);
    }

    public void strongEvidenceForIntendedSerialization(ClassDescriptor c) {
        toldStrongEvidenceForIntendedSerialization.add(c);
    }

    public boolean existsStrongEvidenceForIntendedSerialization(ClassDescriptor c) {
        return toldStrongEvidenceForIntendedSerialization.contains(c);
    }

    public boolean isWrittenOutsideOfInitialization(XField f) {
        return writtenOutsideOfInitializationFields.contains(f);
    }

    public boolean isReflexive(XField f) {
        return reflectiveFields.contains(f);
    }

    public Set<? extends XField> getReadFields() {
        return readFields;
    }

    public Set<? extends XField> getWrittenFields() {
        return writtenFields;
    }

    public boolean isWrittenInConstructor(XField f) {
        return writtenInConstructorFields.contains(f);
    }

    public boolean isWrittenDuringInitialization(XField f) {
        return writtenInInitializationFields.contains(f);
    }

}
