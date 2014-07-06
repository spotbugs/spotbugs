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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.annotation.CheckForNull;
import javax.swing.tree.TreePath;


/**
 * @author pugh
 */
public class FilterActivity {

    private static final HashSet<FilterListener> listeners = new HashSet<FilterListener>();

    public static boolean addFilterListener(FilterListener newListener) {
        return listeners.add(newListener);
    }

    public static void removeFilterListener(FilterListener toRemove) {
        listeners.remove(toRemove);
    }

    public static void notifyListeners(FilterListener.Action whatsGoingOnCode, @CheckForNull TreePath optionalPath) {
        Collection<FilterListener> currentListeners = new ArrayList<FilterListener>(FilterActivity.listeners);
        switch (whatsGoingOnCode) {
        case FILTERING:
        case UNFILTERING:
            for (FilterListener i : currentListeners) {
                i.clearCache();
            }
            break;

        }
        MainFrame.getInstance().updateStatusBar();
    }

    public static class FilterActivityNotifier {
        public void notifyListeners(FilterListener.Action whatsGoingOnCode, TreePath optionalPath) {
            FilterActivity.notifyListeners(whatsGoingOnCode, optionalPath);
        }
    }

}
