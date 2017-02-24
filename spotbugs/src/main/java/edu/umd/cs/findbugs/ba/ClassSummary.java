/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class ClassSummary {
    private final Map<ClassDescriptor, ClassDescriptor> map = new HashMap<ClassDescriptor, ClassDescriptor>();

    private final Set<ClassDescriptor> veryFunky = new HashSet<ClassDescriptor>();

    public boolean mightBeEqualTo(ClassDescriptor checker, ClassDescriptor checkee) {
        return checkee.equals(map.get(checker)) || veryFunky.contains(checker);
    }

    public void checksForEqualTo(ClassDescriptor checker, ClassDescriptor checkee) {
        ClassDescriptor existing = map.get(checker);
        if (checkee.equals(existing)) {
            return;
        } else if (existing != null) {
            veryFunky.add(checker);
        } else {
            map.put(checker, checkee);
        }
    }

}
