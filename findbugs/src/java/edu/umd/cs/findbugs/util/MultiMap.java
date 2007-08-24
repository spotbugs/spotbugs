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

import edu.umd.cs.findbugs.TigerSubstitutes;
/**
 * @author pugh
 */
public class MultiMap<K,  V> {
	final Class<? extends Collection<V>> containerClass;
   @SuppressWarnings("unchecked")
   public  MultiMap(Class<? extends Collection> c) {
		containerClass = (Class<? extends Collection<V>>) c;
	}
	private Collection<V> makeCollection() {
		try {
			return containerClass.newInstance();
		} catch (InstantiationException e) {
		  throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	Map<K,  Collection<V>> map = new HashMap<K,  Collection<V>>();
	public Collection<? extends K> keySet() {
		return map.keySet();
	}
	public void clear() {
		map.clear();
	}
	public void add(K k, V v) {
		Collection<V> s = map.get(k);
		if (s == null) {
			s = makeCollection();
			map.put(k, s);
		}
		s.add(v);
	}
	public void remove(K k, V v) {
		Collection<V> s = map.get(k);
		if (s != null) {
			s.remove(v);
			if (s.isEmpty()) map.remove(k);
		}
	}
	public void removeAll(K k) {
		map.remove(k);
	}
	public Collection<V> get(K k) {
		Collection<V> s = map.get(k);
		if (s != null) return s;
		return TigerSubstitutes.emptySet();
		}

}
