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


/**
 * @author pugh
 */
public class ObjectTypeFactory {

	private static Map<String, ObjectType> map = new HashMap<String, ObjectType>();

	public static ObjectType getInstance(String s) {
		if (s.indexOf("/") >= 0) {
			s = s.replace('/','.');
		}

		ObjectType result = map.get(s);
		if (result != null) return result;
		result = new ObjectType(s);
		map.put(s, result);
		return result;
	}

}
