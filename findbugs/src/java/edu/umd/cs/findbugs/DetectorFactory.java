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
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A DetectorFactory is responsible for creating instances of Detector objects
 * and for maintaining meta-information about the detector class.
 *
 * @see Detector
 * @author David Hovemeyer
 */
public class DetectorFactory {
	private final Class detectorClass;
	private boolean enabled;
	private boolean defEnabled;
	private final String speed;
	private final String reports;
	private final String requireJRE;
	private String detailHTML;

	/**
	 * Constructor.
	 * @param detectorClass the Class object of the Detector
	 * @param enabled true if the Detector is enabled by default, false if disabled
	 * @param speed a string describing roughly how expensive the analysis performed
	 *   by the detector is; suggested values are "fast", "moderate", and "slow"
	 * @param reports comma separated list of bug pattern codes reported
	 *   by the detector; empty if unknown
	 * @param requireJRE string describing JRE version required to run the
	 *   the detector: e.g., "1.5"
	 */
	public DetectorFactory(Class detectorClass, boolean enabled, String speed, String reports,
		String requireJRE) {
		this.detectorClass = detectorClass;
		this.enabled = enabled;
		this.defEnabled = enabled;
		this.speed = speed;
		this.reports = reports;
		this.requireJRE = requireJRE;
	}

	private static final Class[] constructorArgTypes = new Class[]{BugReporter.class};

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

	/** Set the enabled status of the factory. */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
 
	/** Is this factory enabled by default */
	public boolean isDefaultEnabled() {
		return defEnabled;
	}

	/** Get the speed of the Detector produced by this factory. */
	public String getSpeed() {
		return speed;
	}

	/** Get list of bug pattern codes reported by the detector: blank if unknown. */
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

	/** Get an HTML document describing the Detector. */
	public String getDetailHTML() {
		return detailHTML;
	}

	/** Set the HTML document describing the Detector. */
	public void setDetailHTML(String detailHTML) {
		this.detailHTML = detailHTML;
	}

	/**
	 * Create a Detector instance.
	 * @param bugReporter the BugReported to be used to report bugs
	 * @return the Detector
	 */
	public Detector create(BugReporter bugReporter) {
		try {
			Constructor constructor = detectorClass.getConstructor(constructorArgTypes);
			return (Detector) constructor.newInstance(new Object[] {bugReporter});
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
