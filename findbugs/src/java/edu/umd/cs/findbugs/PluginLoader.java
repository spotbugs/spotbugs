/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import java.net.*;
import java.io.*;
import org.dom4j.*;
import org.dom4j.io.*;

public class PluginLoader extends URLClassLoader {
	private Document pluginDescriptor; // a.k.a, "findbugs.xml"

	public PluginLoader(URL url) throws PluginException {
		super(new URL[]{url});
		createFactories();
	}

	public DetectorFactory[] getDetectorFactoryList() {
		return null;
	}

	public BugPattern[] getBugPatternList() {
		return null;
	}

	private void createFactories() throws PluginException {

		try {
			URL descriptorURL = findResource("findbugs.xml");
			if (descriptorURL == null)
				throw new PluginException("Couldn't find \"findbugs.xml\" in plugin");

			SAXReader reader = new SAXReader();
			pluginDescriptor = reader.read(descriptorURL);
		} catch (DocumentException e) {
			throw new PluginException("Couldn't parse \"findbugs.xml\"", e);
		}

	}


	public static void main(String[] argv) {
		try {

			if (argv.length != 1) {
				System.out.println("Usage: " + PluginLoader.class.getName() + " <url>");
				System.exit(1);
			}

			URL url = new URL(argv[0]);
			PluginLoader loader = new PluginLoader(url);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

// vim:ts=4
