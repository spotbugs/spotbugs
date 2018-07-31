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

package edu.umd.cs.findbugs.gui2;

import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author pugh
 */
public class ViewFilter {

    public ViewFilter(MainFrame mf) {
        this.mf = mf;
    }

    interface ViewFilterEnum {
        boolean show(MainFrame mf, BugInstance b);
    }

    enum PriorityFilter implements ViewFilterEnum {
        HIGH_PRIORITY(1, "High priority only"), NORMAL_PRIORITY(2, "High and normal priority"), ALL_BUGS(10, "All bug priorities");

        final int maxPriority;
        final String displayName;

        private PriorityFilter(int maxPriority, String displayName) {
            this.maxPriority = maxPriority;
            this.displayName = displayName;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            return b.getPriority() <= maxPriority;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    enum RankFilter implements ViewFilterEnum {
        SCARIEST(4, "Scariest"), SCARY(9, "Scary"), TROUBLING(14, "Troubling"), ALL(Integer.MAX_VALUE, "All bug ranks");
        final int maxRank;

        final String displayName;

        private RankFilter(int maxRank, String displayName) {
            this.maxRank = maxRank;
            this.displayName = displayName;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            int rank = BugRanker.findRank(b);
            return rank <= maxRank;
        }

        @Override
        public String toString() {
            if (maxRank < Integer.MAX_VALUE) {
                return displayName + " (Ranks 1 - " + maxRank + ")";
            }
            return displayName;
        }

    }


    final MainFrame mf;

    RankFilter rank = RankFilter.ALL;

    PriorityFilter priority = PriorityFilter.ALL_BUGS;

    String[] classSearchStrings;


    static final Pattern legalClassSearchString = Pattern.compile("[\\p{javaLowerCase}\\p{javaUpperCase}0-9.$/_]*");

    void setPackagesToDisplay(String value) {
        value = value.replace('/', '.').trim();
        if (value.length() == 0) {
            classSearchStrings = new String[0];
        } else {
            String[] parts = value.split("[ ,:]+");
            for (String p : parts) {
                if (!legalClassSearchString.matcher(p).matches()) {
                    throw new IllegalArgumentException("Classname filter must be legal Java identifier: " + p);
                }
            }

            classSearchStrings = parts;
        }
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
    }

    public RankFilter getRank() {
        return rank;
    }

    public void setRank(RankFilter rank) {
        this.rank = rank;
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

    }

    public PriorityFilter getPriority() {
        return priority;
    }

    public void setPriority(PriorityFilter priority) {
        this.priority = priority;
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

    }

    public String[] getPackagePrefixes() {
        return classSearchStrings;
    }

    public boolean showIgnoringPackagePrefixes(BugInstance b) {
        return rank.show(mf, b) && priority.show(mf, b);
    }

    /**
     * @deprecated Use {@link ClassName#matchedPrefixes(String[],String)}
     *             instead
     */
    @Deprecated
    public static boolean matchedPrefixes(String[] classSearchStrings, @DottedClassName String className) {
        return ClassName.matchedPrefixes(classSearchStrings, className);
    }

    public boolean show(BugInstance b) {

        String className = b.getPrimaryClass().getClassName();

        return ClassName.matchedPrefixes(classSearchStrings, className) && showIgnoringPackagePrefixes(b);

    }

}
