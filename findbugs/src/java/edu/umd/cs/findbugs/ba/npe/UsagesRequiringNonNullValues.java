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

package edu.umd.cs.findbugs.ba.npe;

import java.util.Collection;
import java.util.LinkedList;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * @author pugh
 */
public class UsagesRequiringNonNullValues {
	public static class Pair {
     public final ValueNumber vn;
     public final PointerUsageRequiringNonNullValue pu;
     Pair(ValueNumber vn, PointerUsageRequiringNonNullValue pu) {
         this.vn = vn;
         this.pu = pu;
     }
    }
    MultiMap<Location, Pair> map = new MultiMap<Location, Pair>(LinkedList.class);
	public void add(Location loc, ValueNumber vn, PointerUsageRequiringNonNullValue usage) {
		Pair p = new Pair(vn, usage);
        map.add(loc, p);
        
	}
	public @CheckForNull PointerUsageRequiringNonNullValue get(Location loc, ValueNumber vn) {
        for(Pair p : map.get(loc)) {
            if (p.vn.equals(vn)) return p.pu;
        }
		return null;
	}
	public Collection<? extends Pair> getValueNumbers(Location loc) {
		return map.get(loc);
	}
	

}
