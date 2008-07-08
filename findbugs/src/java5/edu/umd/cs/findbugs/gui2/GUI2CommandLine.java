/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

import edu.umd.cs.findbugs.FindBugsCommandLine;
import java.io.File;
import java.io.IOException;
import javax.swing.UIManager;

/**
 * Command line switches/options for GUI2.
 * 
 * @author David Hovemeyer
 */
public class GUI2CommandLine extends FindBugsCommandLine {
	private float fontSize = 12;
	private boolean docking = true;
	private int priority = Thread.NORM_PRIORITY-1;
	private File saveFile;

	public GUI2CommandLine() {
		addOption("-f", "font size", "set font size");
		addSwitch("-clear", "clear saved GUI settings and exit");
		addOption("-priority", "thread priority", "set analysis thread priority");
		addOption("-loadbugs", "saved analysis results", "load bugs from saved analysis results");
		addSwitch("-d", "disable docking");
		addSwitch("--nodock", "disable docking");
		addSwitchWithOptionalExtraPart("-look", "plastic|gtk|native", "set UI look and feel");
	}

	@Override
	protected void handleOption(String option, String optionExtraPart) {
		if (option.equals("-clear")) {
			GUISaveState.clear();
			System.exit(0);
		} else if (option.equals("-d") || option.equals("--nodock")) {
			docking = false;
		} else if (option.equals("-look")) {
			String arg = optionExtraPart;
			String theme = null;
			
			if (arg.equals("plastic")) {
				// You can get the Plastic look and feel from jgoodies.com:
				//	http://www.jgoodies.com/downloads/libraries.html
				// Just put "plastic.jar" in the lib directory, right next
				// to the other jar files.
				theme = "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel";
			} else if (arg.equals("gtk")) {
				theme = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
			} else if (arg.equals("native")) {
				theme = UIManager.getSystemLookAndFeelClassName();
			} else {
				System.err.println("Style '" + arg + "' not supported");
			}

			if (theme != null) {
				try {
					UIManager.setLookAndFeel(theme);
				} catch (Exception e) {
					System.err.println("Couldn't load " + arg +
						" look and feel: " + e.toString());
				}
			}
		} else {
			super.handleOption(option, optionExtraPart);
		}
	}

	@Override
	protected void handleOptionWithArgument(String option, String argument) throws IOException {
		if (option.equals("-f")) {
			try {
				fontSize = Float.parseFloat(argument);
			} catch (NumberFormatException e) {
				// ignore
			}
		} else if (option.equals("-priority")) {
			try {
				priority = Integer.parseInt(argument);
			} catch (NumberFormatException e) {
				// ignore
			}
		} else if (option.equals("-loadBugs")) {
			saveFile = new File(argument);
			if (!saveFile.exists()) {
				System.err.println("Bugs file \"" + argument + "\" could not be found");
				System.exit(1);
			}
		} else {
			super.handleOptionWithArgument(option, argument);
		}
	}

	public float getFontSize() {
		return fontSize;
	}
	
	public boolean getDocking() {
		return docking;
	}
	
	public void setDocking(boolean docking) {
		this.docking = docking;
	}

	public int getPriority() {
		return priority;
	}

	public File getSaveFile() {
		return saveFile;
	}
}
