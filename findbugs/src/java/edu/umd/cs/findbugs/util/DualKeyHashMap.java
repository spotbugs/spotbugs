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

package edu.umd.cs.findbugs.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author William Pugh
 */
public class DualKeyHashMap<K1, K2, V>  {
	Map<K1, Map<K2, V>> map = new HashMap<K1, Map<K2, V>>();
	
	
	public V get(K1 k1, K2 k2) {
		Map<K2, V> m = map.get(k1);
		if (m == null) return null;
		return m.get(k2);
	}
	public boolean containsKey(K1 k1, K2 k2) {
		Map<K2, V> m = map.get(k1);
		if (m == null) return false;
		return m.containsKey(k2);
	}
	public Map<K2, V> get(K1 k1) {
		Map<K2, V> m = map.get(k1);
		if (m == null) return Collections.<K2, V>emptyMap();
		return m;
	}
	
	public Set<K1> keySet() {
		return map.keySet();
	}
	public V put(K1 k1, K2 k2, V v) {
		Map<K2, V> m = map.get(k1);
		if (m == null) {
			m = new HashMap<K2, V>();
			map.put(k1, m);
		}
		return m.put(k2,v);
	}

}
