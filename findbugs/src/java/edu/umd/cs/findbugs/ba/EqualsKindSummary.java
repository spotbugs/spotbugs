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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.ClassAnnotation;

/**
 * @author pugh
 */
public class EqualsKindSummary {

    final Map<ClassAnnotation, EqualsKindSummary.KindOfEquals> kindMap = new HashMap<ClassAnnotation, EqualsKindSummary.KindOfEquals>();

    public static enum KindOfEquals {
        OBJECT_EQUALS, ABSTRACT_INSTANCE_OF, INSTANCE_OF_EQUALS,INSTANCE_OF_SUPERCLASS_EQUALS, COMPARE_EQUALS, CHECKED_CAST_EQUALS, RETURNS_SUPER, GETCLASS_GOOD_EQUALS, ABSTRACT_GETCLASS_GOOD_EQUALS, GETCLASS_BAD_EQUALS, DELEGATE_EQUALS, TRIVIAL_EQUALS, INVOKES_SUPER, ALWAYS_TRUE, ALWAYS_FALSE, UNKNOWN
    }

    public EqualsKindSummary.KindOfEquals get(ClassAnnotation c) {
        return kindMap.get(c);
    }

    public void put(ClassAnnotation c, EqualsKindSummary.KindOfEquals k) {
        kindMap.put(c, k);
    }
}
