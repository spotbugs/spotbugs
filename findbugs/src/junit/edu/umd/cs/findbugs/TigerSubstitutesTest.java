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

package edu.umd.cs.findbugs;


import java.util.Map;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class TigerSubstitutesTest extends TestCase {
	static class Foo {};
	static Object bar;
	static Object test = new Object() {
        class Bar { };
		{ 
			bar = new Bar();
		}
        @Override
		public String toString() {
			return new Bar().toString();
		}
    };

	public void testGetSimpleName() {
	main(new String[0]);
		check("a");
		check(new int[1]);
		check(new Object[1]);
        check(new String[1]);
		check(new Foo[1]);
		check(Void.TYPE);
		check(Integer.TYPE);
        check(Foo.class);
		check(Map.Entry.class);
		// check(test);
		check(bar);
    }
	public void check(Object o) {
		check(o.getClass());
	}
    public void check(Class c) {
	String sn = c.getSimpleName();
	String ts = TigerSubstitutes.getSimpleName(c);
	System.out.println( sn + " " + ts);
		assertEquals(sn, ts);
	}
	
    public static void main(String args[]) {
		Class c = test.getClass();
		System.out.println(c.getName());
		System.out.println(c.getCanonicalName());
        System.out.println(c.getSimpleName());
		System.out.println(TigerSubstitutes.getSimpleName(c));
		System.out.println(System.getProperty("java.version"));
		System.out.println(System.getProperty("java.vendor"));
        
	}

}
