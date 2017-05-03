/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
public class ObjectTypeFactory {

    private static ThreadLocal<Map<String, ObjectType>> instance = new ThreadLocal<Map<String, ObjectType>>() {
        @Override
        protected Map<String, ObjectType> initialValue() {
            return new HashMap<String, ObjectType>();
        }
    };

    // private Map<String, ObjectType> map = new HashMap<String, ObjectType>();

    public static void clearInstance() {
        instance.remove();
    }

    public static ObjectType getInstance(Class<?> c) {
        return getInstance(c.getName());
    }


    public static ObjectType getInstance(@DottedClassName String s) {
        if (FindBugs.DEBUG && s.startsWith("[")) {
            throw new IllegalArgumentException("Cannot create an ObjectType to represent an array type: " + s);
        }
        if (s.endsWith(";")) {
            throw new IllegalArgumentException(s);
        }
        if (s.indexOf('/') >= 0) {
            s = s.replace('/', '.');
        }

        Map<String, ObjectType> map = instance.get();
        ObjectType result = map.get(s);
        if (result != null) {
            return result;
        }
        result = ObjectType.getInstance(s);
        map.put(s, result);
        return result;
    }

}
