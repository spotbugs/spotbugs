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

package edu.umd.cs.findbugs;

import java.awt.GraphicsEnvironment;
import java.io.IOException;

/**
 * Class to launch the appropriate GUI
 * @author pugh
 */
public class LaunchAppropriateUI {
	public static void main(String args[]) throws Exception {
		if (GraphicsEnvironment.isHeadless())
			FindBugs2.main(args);
		else {
			boolean useGui1 = false;
			String version = System.getProperty("java.version");
			if ("1.5".compareTo(version) > 0)
				useGui1 = true;

			try {
				Class.forName("edu.umd.cs.findbugs.gui2.MainFrame", false,
						LaunchAppropriateUI.class.getClassLoader());
			} catch (ClassNotFoundException e) {
				useGui1 = true;
			}

			if (useGui1)
				edu.umd.cs.findbugs.gui.FindBugsFrame.main(args);
			else
				edu.umd.cs.findbugs.gui2.Driver.main(args);
		}
	}
}
