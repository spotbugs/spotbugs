/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * A DetectorFactory is responsible for creating instances of Detector objects
 * and for maintaining meta-information about the detector class.
 *
 * @author David Hovemeyer
 * @see Detector
 */
public class DetectorFactory {
	private Plugin plugin;
	private final Class detectorClass;
	private boolean enabled;
	private boolean defEnabled;
	private final String speed;
	private final String reports;
	private final String requireJRE;
	private String detailHTML;
	private int priorityAdjustment;
	private boolean hidden;
	private boolean firstInPass;

	/**
	 * Constructor.
	 *
	 * @param plugin        the Plugin the Detector is part of
	 * @param detectorClass the Class object of the Detector
	 * @param enabled       true if the Detector is enabled by default, false if disabled
	 * @param speed         a string describing roughly how expensive the analysis performed
	 *                      by the detector is; suggested values are "fast", "moderate", and "slow"
	 * @param reports       comma separated list of bug pattern codes reported
	 *                      by the detector; empty if unknown
	 * @param requireJRE    string describing JRE version required to run the
	 *                      the detector: e.g., "1.5"
	 */
	public DetectorFactory(Plugin plugin,
                           Class detectorClass, boolean enabled, String speed, String reports,
	                       String requireJRE) {
		this.plugin = plugin;
		this.detectorClass = detectorClass;
		this.enabled = enabled;
		this.defEnabled = enabled;
		this.speed = speed;
		this.reports = reports;
		this.requireJRE = requireJRE;
		this.priorityAdjustment = 0;
		this.hidden = false;
		this.firstInPass = false;
	}

	private static final Class[] constructorArgTypes = new Class[]{BugReporter.class};

	/**
	 * Get the Plugin that this Detector is part of.
	 * 
	 * @return the Plugin this Detector is part of
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Return whether the factory is enabled.
	 * In addition to checked in the "enabled" attribute of the factory,
	 * this method checks that we are running on the minimum JRE
	 * version required by the detector.
	 */
	public boolean isEnabled() {
		if (!enabled)
			return false;
		if (requireJRE.equals(""))
			return true;
		try {
			JavaVersion requiredVersion = new JavaVersion(requireJRE);
			return JavaVersion.getRuntimeVersion().isSameOrNewerThan(requiredVersion);
		} catch (JavaVersionException e) {
			return false;
		}
	}

	/**
	 * Set visibility of the factory (to GUI dialogs to configure detectors).
	 * Invisible detectors are those that are needed behind the scenes,
	 * but shouldn't be explicitly enabled or disabled by the user.
	 *
	 * @param hidden true if this factory should be hidden, false if not
	 */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * Get visibility of the factory (to GUI dialogs to configure detectors).
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * Set the enabled status of the factory.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Is this factory enabled by default
	 */
	public boolean isDefaultEnabled() {
		return defEnabled;
	}
	
	/**
	 * Set the priority adjustment for the detector produced by this factory.
	 * 
	 * @param priorityAdjustment the priority adjustment
	 */
	public void setPriorityAdjustment(int priorityAdjustment) {
		this.priorityAdjustment = priorityAdjustment;
	}
	
	/**
	 * Get the priority adjustment for the detector produced by this factory.
	 * 
	 * @return the priority adjustment
	 */
	public int getPriorityAdjustment() {
		return priorityAdjustment;
	}

	/**
	 * Mark whether or not this detector needs to be first in its analysis pass.
	 *
	 * @param firstInPass true if the detector should be first in its pass,
	 *                    false if it does not
	 */
	public void setFirstInPass(boolean firstInPass) {
		this.firstInPass = firstInPass;
	}

	/**
	 * Return whether or not this detector needs to be first in
	 * its analysis pass.
	 */
	public boolean isFirstInPass() {
		return firstInPass;
	}

	/**
	 * Get the speed of the Detector produced by this factory.
	 */
	public String getSpeed() {
		return speed;
	}

	/**
	 * Get list of bug pattern codes reported by the detector: blank if unknown.
	 */
	public String getReportedBugPatternCodes() {
		return reports;
	}

	/**
	 * Get Collection of all BugPatterns this detector reports.
	 * An empty Collection means that we don't know what kind of
	 * bug patterns might be reported.
	 */
	public Collection<BugPattern> getReportedBugPatterns() {
		List<BugPattern> result = new LinkedList<BugPattern>();
		StringTokenizer tok = new StringTokenizer(reports, ",");
		while (tok.hasMoreTokens()) {
			String type = tok.nextToken();
			BugPattern bugPattern = I18N.instance().lookupBugPattern(type);
			if (bugPattern != null)
				result.add(bugPattern);
		}
		return result;
	}

	/**
	 * Get an HTML document describing the Detector.
	 */
	public String getDetailHTML() {
		return detailHTML;
	}

	/**
	 * Set the HTML document describing the Detector.
	 */
	public void setDetailHTML(String detailHTML) {
		this.detailHTML = detailHTML;
	}

	/**
	 * Create a Detector instance.
	 *
	 * @param bugReporter the BugReported to be used to report bugs
	 * @return the Detector
	 */
	public Detector create(BugReporter bugReporter) {
		try {
			Constructor constructor = detectorClass.getConstructor(constructorArgTypes);
			return (Detector) constructor.newInstance(new Object[]{bugReporter});
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate Detector", e);
		}
	}

	/**
	 * Get the short name of the Detector.
	 * This is the name of the detector class without the package qualification.
	 */
	public String getShortName() {
		String className = detectorClass.getName();
		int endOfPkg = className.lastIndexOf('.');
		if (endOfPkg >= 0)
			className = className.substring(endOfPkg + 1);
		return className;
	}

	/**
	 * Get the full name of the detector.
	 * This is the name of the detector class, with package qualification.
	 */
	public String getFullName() {
		return detectorClass.getName();
	}
}

// vim:ts=4
