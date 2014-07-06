/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005,2008 University of Maryland
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

import java.util.TreeSet;

/**
 * Show command line help.
 *
 * @author David Hovemeyer
 */
public class ShowHelp {

    public static void main(String[] args) {

        System.out.println("FindBugs version " + Version.RELEASE + ", " + Version.WEBSITE);

        DetectorFactoryCollection.instance();
        System.out.println("Command line options");

        TreeSet<FindBugsMain> cmds = new TreeSet<FindBugsMain>();
        for(Plugin p : Plugin.getAllPlugins()) {
            for(FindBugsMain m : p.getAllFindBugsMain()) {
                cmds.add(m);
            }
        }
        for(FindBugsMain m : cmds) {
            System.out.printf("fb %-12s %-12s %s%n", m.cmd, m.kind, m.description);
        }

        //        System.out.println();
        //        System.out.println("GUI Options:");
        //        FindBugsCommandLine guiCmd = new FindBugsCommandLine(true) {
        //        };
        //        guiCmd.printUsage(System.out);
        //        System.out.println();
        //        System.out.println("TextUI Options:");
        //        FindBugs.showCommandLineOptions();
        System.out.println();
        showGeneralOptions();

    }

    public static void showSynopsis() {
        System.out.println("Usage: findbugs [general options] [gui options]");
    }

    public static void showGeneralOptions() {

        System.out.println("General options:");
        System.out.println("  -jvmArgs args    Pass args to JVM");
        System.out.println("  -maxHeap size    Maximum Java heap size in megabytes (default=768)");
        System.out.println("  -javahome <dir>  Specify location of JRE");

    }
}
