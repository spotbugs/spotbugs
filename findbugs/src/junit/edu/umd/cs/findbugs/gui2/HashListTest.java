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

package edu.umd.cs.findbugs.gui2;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class HashListTest extends TestCase {
    
    /**
     * 
     */
    private static final BadHasher ZERO = new BadHasher(0);
    private static final BadHasher ONE = new BadHasher(1);
    
    private static final BadHasher TWO = new BadHasher(2);
    
    private static final BadHasher THREE = new BadHasher(3);
    
    public void testAdd() {
        HashList<Integer> lst = new HashList<Integer>();
        lst.add(0,1);
        lst.add(0,2);
        lst.add(0,3);
        assertEquals(2,lst.indexOf(1));
        assertEquals(1,lst.indexOf(2));
        assertEquals(0,lst.indexOf(3));
    }
    public void testBadHasher() {
        HashList<BadHasher> lst = new HashList<BadHasher>();
        lst.add(0,ZERO);
        lst.add(0,ONE);
        lst.add(1,TWO);
        // lst.add(0,THREE);
        assertEquals(2,lst.indexOf(ZERO));
        assertEquals(0,lst.indexOf(ONE));
        assertEquals(1,lst.indexOf(TWO));
    }
    
    static class BadHasher {
        int value;
        public BadHasher(int value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return Integer.toString(value);
        }
        @Override
        public int hashCode() {
            return 42;
        }
        @Override
        public boolean equals(Object v) {
            if (!(v instanceof BadHasher)) return false;
            return value == ((BadHasher)v).value;
        }
    }
    public void testRetainAllEmptu() {
        HashList<Integer> lst = new HashList<Integer>();
        lst.add(0,1);
        lst.add(0,2);
        lst.add(0,3);
        lst.retainAll(Collections.emptySet());
        assertEquals(0, lst.size());
        
    }

    public void testRetainAllEven() {
        HashList<BadHasher> lst = new HashList<BadHasher>();
        for(int i = 0; i < 20; i++)
            lst.add(new BadHasher(i));
        HashList<BadHasher> even = new HashList<BadHasher>();
        for(int i = 0; i < 20; i+=2)
            even.add(new BadHasher(i));
        lst.retainAll(even);
        assertEquals(10, lst.size());
        
    }

}
