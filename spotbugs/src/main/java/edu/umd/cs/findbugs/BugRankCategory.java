/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import javax.annotation.Nonnull;

/**
 * Smaller value is scarier
 *
 * @see BugRanker
 * @see edu.umd.cs.findbugs.annotations.Confidence
 */
public enum BugRankCategory {

    SCARIEST(4), SCARY(9), TROUBLING(14), OF_CONCERN(BugRanker.VISIBLE_RANK_MAX);

    public final int maxRank;

    @Nonnull
    static public BugRankCategory getRank(int rank) {
        for(BugRankCategory c : values()) {
            if (rank <= c.maxRank) {
                return c;
            }
        }
        throw new IllegalArgumentException("Rank of " + rank + " is outside legal rank");
    }

    private BugRankCategory(int maxRank) {
        this.maxRank = maxRank;
    }

    @Override
    public String toString() {
        if(this == OF_CONCERN) {
            return "Of Concern";
        }
        return name().substring(0,1) + name().toLowerCase().substring(1, name().length());
    }
}
