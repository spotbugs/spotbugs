/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.Plugin;

/**
 * Select all detector factories for reporting detectors.
 * 
 * @author David Hovemeyer
 */
public class ReportingDetectorFactorySelector implements DetectorFactorySelector {
	private Plugin plugin;

	/**
	 * Constructor.
	 * 
	 * @param plugin Plugin containing detector factories to be selected;
	 *               if null, factories from any Plugin may be selected
	 */
	public ReportingDetectorFactorySelector(Plugin plugin) {
		this.plugin = plugin;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.plan.DetectorFactorySelector#selectFactory(edu.umd.cs.findbugs.DetectorFactory)
	 */
	public boolean selectFactory(DetectorFactory factory) {
		return (plugin == null || plugin == factory.getPlugin())
			&& factory.isReportingDetector();
	}

	@Override
		 public String toString() {
		String s = "All reporting detectors";
		if (plugin != null) {
			s += " in plugin " + plugin.getPluginId();
		}
		return s;
	}
}
