/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.view.explorer.test;

import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import de.tobject.findbugs.view.explorer.FilterBugsDialog;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugPattern;

/**
 * Test subclass of FilterBugsDialog that overrides the opening behaviour for
 * testing purposes.
 * 
 * @author Tomás Pollak
 */
public class FilterBugsDialogTestSubclass extends FilterBugsDialog {

    public FilterBugsDialogTestSubclass(Shell parentShell, Set<BugPattern> filteredPatterns, Set<BugCode> filteredTypes) {
        super(parentShell, filteredPatterns, filteredTypes);
    }

    /**
     * Accessor method for tests to simulate the user selecting a bug code.
     * 
     * @param code
     *            The BugCode to select.
     */
    public void addBugCodeToFilter(BugCode code) {
        elementChecked(code, true);
    }

    /**
     * Accessor method for tests to simulate the user selecting a bug pattern.
     * 
     * @param pattern
     *            The BugPattern to select.
     */
    public void addBugPatternToFilter(BugPattern pattern) {
        elementChecked(pattern, true);
    }

    @Override
    public int open() {
        setBlockOnOpen(false);
        return super.open();
    }
}
