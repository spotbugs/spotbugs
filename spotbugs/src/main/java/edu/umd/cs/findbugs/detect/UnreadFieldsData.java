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
    final Map<XField, Set<ProgramPoint>> assumedNonNull = new HashMap<XField, Set<ProgramPoint>>();

    final  Map<XField, ProgramPoint> threadLocalAssignedInConstructor = new HashMap<XField, ProgramPoint>();

    final Set<XField> nullTested = new HashSet<XField>();

    final Set<XField> containerFields = new TreeSet<XField>();

    final MultiMap<XField, String> unknownAnnotation = new MultiMap<XField, String>(LinkedList.class);

    final Set<String> abstractClasses = new HashSet<String>();

    final Set<String> hasNonAbstractSubClass = new HashSet<String>();

    final Set<String> classesScanned = new HashSet<String>();

    final Set<XField> fieldsOfNativeClasses = new HashSet<XField>();

    final Set<XField> reflectiveFields = new HashSet<XField>();

    final Set<XField> fieldsOfSerializableOrNativeClassed = new HashSet<XField>();

    final  Set<XField> staticFieldsReadInThisMethod = new HashSet<XField>();

    final  Set<XField> allMyFields = new TreeSet<XField>();

    final Set<XField> myFields = new TreeSet<XField>();

    final  Set<XField> writtenFields = new HashSet<XField>();


    final Map<XField, SourceLineAnnotation> fieldAccess = new HashMap<XField, SourceLineAnnotation>();

    final  Set<XField> writtenNonNullFields = new HashSet<XField>();

    final  Set<String> calledFromConstructors = new HashSet<String>();

    final Set<XField> writtenInConstructorFields = new HashSet<XField>();

    final Set<XField> writtenInInitializationFields = new HashSet<XField>();

    final Set<XField> writtenOutsideOfInitializationFields = new HashSet<XField>();

    final  Set<XField> readFields = new HashSet<XField>();

    final  Set<XField> constantFields = new HashSet<XField>();

    final Set<String> needsOuterObjectInConstructor = new HashSet<String>();

    final  Set<String> innerClassCannotBeStatic = new HashSet<String>();

    final   HashSet<ClassDescriptor> toldStrongEvidenceForIntendedSerialization = new HashSet<ClassDescriptor>();

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
