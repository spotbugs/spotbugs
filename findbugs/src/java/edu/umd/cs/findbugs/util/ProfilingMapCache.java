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

import java.util.Map;

/**
 * @author pugh
 */
public class ProfilingMapCache<K,V> extends MapCache<K,V> {

	final String name;
	
	public ProfilingMapCache(int maxCapacity, String name) {
		super(maxCapacity);
		this.name = name;
		count = new int[maxCapacity];
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
            public void run() {
				System.out.println("Profile for map cache " + ProfilingMapCache.this.name);
				for(int i = 0; i < count.length; i++)
					System.out.printf("%4d %5d\n", i, count[i]);
			}
		});
	}
	int [] count;
	@Override
    public V get(Object k) { 

		int age = count.length-1;
		for(Map.Entry<K,V> e : entrySet()) {
			if (e.getKey().equals(k)) {
				count[age]++;
				if (age > 20) {
					new RuntimeException("Reusing value from " + age + " steps ago in " + name).printStackTrace(System.out);
				}
				break;
			}
			age--;
		}
		return super.get(k);
	}
	public String getStatistics() {
		StringBuffer b = new StringBuffer();
		for(int c : count) 
			b.append(c).append(" ");
		return b.toString();
	}
}
