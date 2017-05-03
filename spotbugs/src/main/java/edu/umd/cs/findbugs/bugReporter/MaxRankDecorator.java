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

package edu.umd.cs.findbugs.bugReporter;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ComponentPlugin;

/**
 * @author pugh
 */
public class MaxRankDecorator extends BugReporterDecorator {

    final int maxRank;

    public MaxRankDecorator(ComponentPlugin<BugReporterDecorator> plugin, BugReporter delegate) {
        super(plugin, delegate);
        maxRank = Integer.parseInt(plugin.getProperties().getProperty("maxRank"));
    }

    @Override
    public void reportBug(@Nonnull BugInstance bugInstance) {
        int rank = bugInstance.getBugRank();
        if (rank <= maxRank) {
            getDelegate().reportBug(bugInstance);
        }

    }

}
