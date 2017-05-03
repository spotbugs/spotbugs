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

import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
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

        @Override
        public String toString() {
            return vn.toString();
        }
    }

    MultiMap<Integer, Pair> map = new MultiMap<Integer, Pair>(LinkedList.class);

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<Integer, Collection<Pair>> e : map.asMap().entrySet()) {
            buf.append(e).append("\n");
        }
        return buf.toString();
    }

    public void add(Location loc, ValueNumber vn, PointerUsageRequiringNonNullValue usage) {
        Pair p = new Pair(vn, usage);
        if (DerefFinder.DEBUG) {
            System.out.println("At " + loc + " adding dereference " + p);
        }

        map.add(loc.getHandle().getPosition(), p);
    }

    public @CheckForNull
    PointerUsageRequiringNonNullValue get(Location loc, ValueNumber vn, ValueNumberDataflow vnaDataflow) {
        // PointerUsageRequiringNonNullValue secondBest = null;
        MergeTree mergeTree = vnaDataflow.getAnalysis().getMergeTree();
        for (Pair p : map.get(loc.getHandle().getPosition())) {
            if (p.vn.equals(vn)) {
                return p.pu;
            }
            if (!p.vn.hasFlag(ValueNumber.PHI_NODE)) {
                continue;
            }
            BitSet inputs = mergeTree.getTransitiveInputSet(p.vn);
            if (inputs.get(vn.getNumber())) {
                return p.pu;
            }
        }
        return null;
    }

    public Collection<? extends Pair> getPairs(Integer loc) {
        return map.get(loc);
    }

}
