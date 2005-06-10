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
import java.util.ArrayList;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.config.CommandLine;

public abstract class FindBugsCommandLine extends CommandLine {
	
	public FindBugsCommandLine() {
		addOption("-home", "home directory", "specify FindBugs home directory");
		addOption("-pluginList", "jar1[" + File.pathSeparator + "jar2...]",
		        "specify list of plugin Jar files to load");
	}

	//@Override
	protected void handleOption(String option, String optionExtraPart) {
		throw new IllegalStateException();
	}

	//@Override
	protected void handleOptionWithArgument(String option, String argument) throws IOException {
		if (option.equals("-home")) {
			FindBugs.setHome(argument);
		} else if (option.equals("-pluginList")) {
			String pluginListStr = argument;
			ArrayList<File> pluginList = new ArrayList<File>();
			StringTokenizer tok = new StringTokenizer(pluginListStr, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				pluginList.add(new File(tok.nextToken()));
			}

			DetectorFactoryCollection.setPluginList((File[]) pluginList.toArray(new File[pluginList.size()]));
		} else {
			throw new IllegalStateException();
		}
	}
}
