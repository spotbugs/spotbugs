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

package edu.umd.cs.findbugs.launchGUI;

import java.awt.GraphicsEnvironment;

import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.gui2.FindBugsLayoutManagerFactory;
import edu.umd.cs.findbugs.gui2.GUISaveState;
import edu.umd.cs.findbugs.gui2.MainFrame;
import edu.umd.cs.findbugs.gui2.SplitLayout;

/**
 * @author pugh
 */
public class LaunchGUI {

    public static void launchGUI(SortedBugCollection bugs) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Running in GUI headless mode, can't open GUI");
        }
        GUISaveState.loadInstance();
        try {
            FindBugsLayoutManagerFactory factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
            MainFrame.makeInstance(factory);
            MainFrame instance = MainFrame.getInstance();
            instance.waitUntilReady();
            instance.openBugCollection(bugs);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
