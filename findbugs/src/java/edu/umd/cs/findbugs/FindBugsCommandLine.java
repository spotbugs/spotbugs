/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Base class for FindBugs command line classes.
 * Handles all shared switches/options.
 * 
 * @author David Hovemeyer
 */
public abstract class FindBugsCommandLine extends CommandLine {

	/**
	 * Analysis settings to configure the analysis effort.
	 */
	protected AnalysisFeatureSetting[] settingList = FindBugs.DEFAULT_EFFORT;

	/**
	 * Project to analyze.
	 */
	protected Project project = new Project();

	/**
	 * Constructor.
	 * Adds shared options/switches.
	 */
	public FindBugsCommandLine() {
		addOption("-project", "project", "analyze given project");
		addOption("-home", "home directory", "specify FindBugs home directory");
		addOption("-pluginList", "jar1[" + File.pathSeparator + "jar2...]",
				"specify list of plugin Jar files to load");
		addSwitchWithOptionalExtraPart("-effort", "min|default|max", "set analysis effort level");
		addSwitch("-adjustExperimental", "lower priority of experimental Bug Patterns");
		addSwitch("-workHard", "ensure analysis effort is at least 'default'");
		addSwitch("-conserveSpace", "same as -effort:min (for backward compatibility)");
	}

	public AnalysisFeatureSetting[] getSettingList() {
		return settingList;
	}

	public Project getProject() {
		return project;
	}


	@Override
	protected void handleOption(String option, String optionExtraPart) {
		if (option.equals("-effort")) {
			if (optionExtraPart.equals("min")) {
				settingList = FindBugs.MIN_EFFORT;
			} else if (optionExtraPart.equals("less")) {
				settingList = FindBugs.LESS_EFFORT;
			} else if (optionExtraPart.equals("default")) {
				settingList = FindBugs.DEFAULT_EFFORT;
			} else if (optionExtraPart.equals("more")) {
				settingList = FindBugs.MORE_EFFORT;
			} else if (optionExtraPart.equals("max")) {
				settingList = FindBugs.MAX_EFFORT;
			} else {
				throw new IllegalArgumentException("-effort:<value> must be one of min,default,more,max");
			}
		} else if (option.equals("-workHard")) {
			if (settingList != FindBugs.MAX_EFFORT)
				settingList = FindBugs.MORE_EFFORT;

		} else if (option.equals("-conserveSpace")) {
			settingList = FindBugs.MIN_EFFORT;
		} else if (option.equals("-adjustExperimental")) {
			BugInstance.setAdjustExperimental(true);
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	protected void handleOptionWithArgument(String option, String argument) throws IOException {
		if (option.equals("-home")) {
			FindBugs.setHome(argument);
		} else if (option.equals("-pluginList")) {
			String pluginListStr = argument;
			ArrayList<URL> pluginList = new ArrayList<URL>();
			StringTokenizer tok = new StringTokenizer(pluginListStr, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				pluginList.add(new File(tok.nextToken()).toURL());
			}

			DetectorFactoryCollection.rawInstance().setPluginList(pluginList.toArray(new URL[pluginList.size()]));
		} else if (option.equals("-project")) {
			String projectFile = argument;

			// Convert project file to be an absolute path
			projectFile = new File(projectFile).getAbsolutePath();

			try {
				project = new Project();
				project.read(projectFile);
			} catch (IOException e) {
				System.err.println("Error opening " + projectFile);
				e.printStackTrace(System.err);
				throw e;
			}
		} else {
			throw new IllegalStateException();
		}
	}
}
