/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006,2008 University of Maryland
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

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to launch the appropriate textUI or GUI.
 * This class is the Main-Class in the findbugs.jar
 * manifest, and is responsible for running an
 * appropriate main() method.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class LaunchAppropriateUI {
	/**
	 * UI code for the text (command line) UI.
	 */
	public static final int TEXTUI = 0;
	
	/**
	 * UI code for the old Swing GUI.
	 * This is deprecated now.
	 */
	public static final int GUI1 = 1;
	
	/**
	 * UI code for the new Swing GUI.
	 */
	public static final int GUI2 = 2;
	
	/**
	 * UI code for displaying command line help.
	 */
	public static final int SHOW_HELP = 1000;
	
	/**
	 * UI code for displaying command line version information.
	 */
	public static final int SHOW_VERSION = 1001;

	/**
	 * GUI2 is the default UI.
	 * This means if you double-click on findbugs.jar,
	 * GUI2 is started.
	 */
	public static final int DEFAULT_UI = GUI2;
	
	/**
	 * Map of UI name strings to integer UI codes.
	 */
	public static final Map<String, Integer> uiNameToCodeMap;
	static {
		uiNameToCodeMap = new HashMap<String, Integer>();
		uiNameToCodeMap.put("textui", TEXTUI);
		uiNameToCodeMap.put("gui1", GUI1);
		uiNameToCodeMap.put("gui2", GUI2);
		uiNameToCodeMap.put("help", SHOW_HELP);
		uiNameToCodeMap.put("version", SHOW_VERSION);
	}
	
	public static void main(String args[]) throws Exception {
		int launchProperty = getLaunchProperty();

		// Sanity-check the loaded BCEL classes
		if(!CheckBcel.check()) {
			System.exit(1);
		}

		if (GraphicsEnvironment.isHeadless() || launchProperty == TEXTUI) {
			FindBugs2.main(args);
		} else if (launchProperty == SHOW_HELP) {
			ShowHelp.main(args);
		} else if (launchProperty == SHOW_VERSION) {
			Version.main(new String[]{"-release"});
		} else {
			String version = System.getProperty("java.version");

			Class<?> launchClass = null;
			if ("1.5".compareTo(version) <= 0) {
				try {
					launchClass = Class.forName("edu.umd.cs.findbugs.gui2.Driver", false,
						LaunchAppropriateUI.class.getClassLoader());
				} catch (ClassNotFoundException e) {
					assert true;
				}
			}
			
			if (launchClass == null || launchProperty == GUI1) {
				launchClass = edu.umd.cs.findbugs.gui.FindBugsFrame.class;
			}

			Method mainMethod = launchClass.getMethod("main", args.getClass());
			mainMethod.invoke(null, (Object) args);
		}
	}

	/**
	 * User should set the <code>findbugs.launchUI</code> system property
	 * to one of the following values:
	 * 
	 * <ul>
	 * <li>-Dfindbugs.launchUI=textui for textui, </li>
	 * <li> -Dfindbugs.launchUI=gui1 for the original swing gui, </li>
	 * <li> -Dfindbugs.launchUI=gui2 for the new swing gui, </li>
	 * <li> -Dfindbugs.launchUI=version for the ShowVersion main() method, or </li>
	 * <li> -Dfindbugs.launchUI=help for the ShowHelp main() method. </li>
	 * </ul>
	 * 
	 * Any other value (or the absence of any value) will
	 *  not change the default behavior, which is to launch
	 *  the newer "gui2" on systems that support it.
	 *  
	 * @return an integer UI code:
	 *         TEXTUI, GUI1, GUI2, SHOW_VERSION, SHOW_HELP,
	 *         or possibly another user-set int value
	 */
	public static int getLaunchProperty() {
		String s = System.getProperty("findbugs.launchUI", "gui2");

		// See if the property value is one of the human-readable
		// UI names.
		if (uiNameToCodeMap.containsKey(s)) {
			return uiNameToCodeMap.get(s);
		}

		// Fall back: try to parse it as an integer.
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return 2;
		}
	}

}
