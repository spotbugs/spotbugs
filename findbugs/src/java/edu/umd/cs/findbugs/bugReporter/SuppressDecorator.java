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

package edu.umd.cs.findbugs.bugReporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;


import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
public class SuppressDecorator extends BugReporterDecorator {

	final String category;
	final String adjustmentSource;
	
	HashSet<String> check = new HashSet<String>();
	HashSet<String> dontCheck = new HashSet<String>();
	final boolean byPackage;
	
	public SuppressDecorator(BugReporterPlugin plugin, BugReporter delegate) {
		super(plugin, delegate);
		category = plugin.getProperties().getProperty("category");
		if (I18N.instance().getBugCategory(category) == null)
			throw new IllegalArgumentException("Unable to find category " + category);
		adjustmentSource =  plugin.getProperties().getProperty("source");
		if (adjustmentSource == null) {
			byPackage = false;
			return;
		}
		try {
			URL u;
			
			if (adjustmentSource.startsWith("file:") || adjustmentSource.startsWith("http:")
			        || adjustmentSource.startsWith("https:"))
				u = new URL(adjustmentSource);
			else {
				u = plugin.getPlugin().getPluginLoader().getResource(adjustmentSource);
				if (u == null)
					u = PluginLoader.getCoreResource(adjustmentSource);
				if (u == null) {
					byPackage = false;
					return;
				}
					
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
			while (true) {
				String s = in.readLine();
				if (s == null)
					break;
				String packageName = s.substring(1).trim();
				if (s.charAt(0) == '+') 
					check.add(packageName);
				else if (s.charAt(0) == '-') 
					dontCheck.add(packageName);
				else throw new IllegalArgumentException("Can't parse " + category + " filter line: " + s);
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to load " + category + " filters from " + adjustmentSource, e);
		}
		byPackage = true;
	}

	@Override
    public void reportBug(BugInstance bugInstance) {
		
		if (!category.equals( bugInstance.getBugPattern().getCategory())) {
			getDelegate().reportBug(bugInstance);
			return;
		}
		if (!byPackage)
			return;
		
		ClassAnnotation c = bugInstance.getPrimaryClass();
		@DottedClassName String packageName = c.getPackageName();
		
		while (true) {
			if (check.contains(packageName)) {
				getDelegate().reportBug(bugInstance);
				return;
			} else if (dontCheck.contains(packageName))  {
				return;
			}
			int i = packageName.lastIndexOf('.');
			if (i < 0)
				return;
			packageName = packageName.substring(0,i);
		}
		

	}

}
