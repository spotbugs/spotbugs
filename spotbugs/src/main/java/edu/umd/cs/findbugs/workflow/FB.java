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

package edu.umd.cs.findbugs.workflow;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugsMain;
import edu.umd.cs.findbugs.Plugin;

/**
 * @author pugh
 */
public class FB {

    public static void main(String args[]) throws Throwable {

        String cmd;
        String a[];
        if (args.length == 0) {
            cmd = "help";
            a = new String[0];
        } else {
            cmd = args[0];
            a = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                a[i - 1] = args[i];
            }
        }

        DetectorFactoryCollection.instance();
        for (Plugin plugin : Plugin.getAllPlugins()) {
            FindBugsMain main = plugin.getFindBugsMain(cmd);
            if (main != null) {
                try {
                    main.invoke(a);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
                return;
            }

        }

        throw new IllegalArgumentException("Unable to find FindBugs main for " + cmd);
    }

}
