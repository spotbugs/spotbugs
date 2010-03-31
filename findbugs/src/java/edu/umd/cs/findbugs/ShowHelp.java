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


/**
 * Show command line help.
 * 
 * @author David Hovemeyer
 */
public class ShowHelp {
	public static void main(String[] args) {
		
		System.out.println("FindBugs version " + Version.RELEASE + ", " + Version.WEBSITE);
		showGeneralOptions();
		System.out.println("GUI Options:");
		FindBugsCommandLine guiCmd = new FindBugsCommandLine(true){};
		guiCmd.printUsage(System.out);
		System.out.println();
		System.out.println("Command line Options:");
		FindBugs.showCommandLineOptions();
	}

	public static void showSynopsis() {
		System.out.println("Usage: findbugs [general options] [gui options]");
	}
	public static void showGeneralOptions() {
		System.out.println("General options:");
		System.out.println("  -gui             Use the Graphical UI (default behavior)");
		System.out.println("  -textui          Use the Text UI");
		System.out.println("  -jvmArgs args    Pass args to JVM");
		System.out.println("  -maxHeap size    Maximum Java heap size in megabytes (default=768)");
		System.out.println("  -javahome <dir>  Specify location of JRE");
		System.out.println("  -help            Display command line options");
		System.out.println("  -debug           Enable debug tracing in FindBugs");
	}
}
