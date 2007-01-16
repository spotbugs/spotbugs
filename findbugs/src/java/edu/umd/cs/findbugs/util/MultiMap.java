/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.util;
import java.util.*;
/**
 * @author pugh
 */
public class MultiMap<K,  V> {
	Map<K,  Set<V>> map = new HashMap<K, Set<V>>();
	public void add(K k, V v) {
		Set<V> s = map.get(k);
		if (s == null) {
			s = new HashSet<V>();
			map.put(k, s);
		}
		s.add(v);
	}
	public void remove(K k, V v) {
		Set<V> s = map.get(k);
		if (s != null) {
			s.remove(v);
			if (s.isEmpty()) map.remove(k);
		}
	}
	public Set<V> get(K k) {
		Set<V> s = map.get(k);
		if (s != null) return s;
		return Collections.emptySet();
		}

}
