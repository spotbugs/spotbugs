/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom�s Pollak
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
package de.tobject.findbugs.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Enum for the different test scenarios in FindBugs. Each test scenario has
 * information of the input files, the expected bugs and markers, and so on.
 *
 * @author Tom�s Pollak
 */
public enum TestScenario {
    DEFAULT(new String[] { "/defaultScenario" }, false, new String[] { "URF_UNREAD_FIELD", "DM_STRING_CTOR" }, 2),
    QUICKFIX(new String[] { "/quickfixScenario" }, false, new String[] {}, 0),
    QUICKFIX_WITH_JUNIT(new String[] { "/quickfixScenario" }, true, new String[] {}, 0),
    MULTIQUICKFIX(new String[] { "/multiQuickfixScenario" }, false, new String[]{}, 0),
    JDT(new String[] { "/jdtScenario" }, false, new String[] {}, 0),
    TWO_SRC_FOLDERS(new String[] { "/defaultScenario", "secondSrcScenario" }, false, new String[] {"URF_UNREAD_FIELD", "DM_STRING_CTOR", "DM_NUMBER_CTOR" }, 2);

    private String[] testFilesPaths;

    private boolean usesJUnit;

    private final Map<String, Integer> visibleBugsHistogram = new HashMap<String, Integer>();

    private int visibleBugsCount;

    private int filteredBugsCount;

    private TestScenario(String[] testFilesPaths, boolean usesJUnit, String[] visibleBugsArray, int filteredBugsCount) {
        this.testFilesPaths = testFilesPaths;
        this.usesJUnit = usesJUnit;
        initializeBugsHistogram(visibleBugsArray);
        visibleBugsCount = visibleBugsArray.length;
        this.filteredBugsCount = filteredBugsCount;
    }

    public int getFilteredBugsCount() {
        return filteredBugsCount;
    }

    public String[] getTestFilesPaths() {
        return testFilesPaths;
    }

    public int getVisibleBugFrequency(String bugPattern) {
        if (visibleBugsHistogram.containsKey(bugPattern)) {
            return visibleBugsHistogram.get(bugPattern);
        }
        return 0;
    }

    public Set<String> getVisibleBugs() {
        return visibleBugsHistogram.keySet();
    }

    public int getVisibleBugsCount() {
        return visibleBugsCount;
    }

    public boolean usesJUnit() {
        return usesJUnit;
    }

    private void initializeBugsHistogram(String[] visibleBugsArray) {
        // Assemble the bug cardinality map from the bug array
        for (int i = 0; i < visibleBugsArray.length; i++) {
            if (!visibleBugsHistogram.containsKey(visibleBugsArray[i])) {
                visibleBugsHistogram.put(visibleBugsArray[i], 0);
            }
            Integer count = visibleBugsHistogram.get(visibleBugsArray[i]);
            visibleBugsHistogram.put(visibleBugsArray[i], count + 1);
        }
    }
}
