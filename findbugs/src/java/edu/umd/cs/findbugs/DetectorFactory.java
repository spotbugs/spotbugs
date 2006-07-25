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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * A DetectorFactory is responsible for creating instances of Detector objects
 * and for maintaining meta-information about the detector class.
 *
 * @author David Hovemeyer
 * @see Detector
 */
public class DetectorFactory {
	private static final boolean DEBUG_JAVA_VERSION = Boolean.getBoolean("findbugs.debug.javaversion");

	private static final Class[] constructorArgTypes = new Class[]{BugReporter.class};

	static class ReflectionDetectorCreator implements IDetectorCreator {
		private Class<? extends Detector> detectorClass;
		private Method setAnalysisContext;
		
		ReflectionDetectorCreator(Class<? extends Detector> detectorClass) {
			this.detectorClass = detectorClass;

			try {
				setAnalysisContext = detectorClass.getDeclaredMethod(
						"setAnalysisContext", new Class[]{AnalysisContext.class});
			} catch (NoSuchMethodException e) {
				// Ignore
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.IDetectorCreator#createDetector(edu.umd.cs.findbugs.BugReporter)
		 */
		public Detector2 createDetector(BugReporter bugReporter) {
			try {
				Constructor<? extends Detector> constructor = detectorClass.getConstructor(constructorArgTypes);
				Detector detector = constructor.newInstance(new Object[]{bugReporter});
				
				// Backwards-compatibility: if the Detector has a setAnalysisContext()
				// method, call it, passing the current AnalysisContext.  We do this
				// because some released versions of FindBugs had a Detector
				// interface which specified this method (and ensured it was called
				// before the Detector was used to analyze any code).
				if (setAnalysisContext != null) {
					setAnalysisContext.invoke(detector, new Object[]{AnalysisContext.currentAnalysisContext()});
				}
				
				return new DetectorToDetector2Adapter(detector);
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate Detector", e);
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.IDetectorCreator#getDetectorClass()
		 */
		public Class<? extends Detector> getDetectorClass() {
			return detectorClass;
		}
	}
	
	private Plugin plugin;
	private final IDetectorCreator detectorCreator;
	private int positionSpecifiedInPluginDescriptor;
	private boolean defEnabled;
	private final String speed;
	private final String reports;
	private final String requireJRE;
	private String detailHTML;
	private int priorityAdjustment;
	private boolean hidden;

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
                           Class<? extends Detector> detectorClass, boolean enabled, String speed, String reports,
	                       String requireJRE) {
		this.plugin = plugin;
		this.detectorCreator = new ReflectionDetectorCreator(detectorClass);
		this.defEnabled = enabled;
		this.speed = speed;
		this.reports = reports;
		this.requireJRE = requireJRE;
		this.priorityAdjustment = 0;
		this.hidden = false;
	}

	/**
	 * Constructor.
	 * 
	 * @param plugin          the Plugin the detector is part of
	 * @param detectorCreator the IDetectorCreator that will create instances of this detector
	 * @param enabled         true if the detector is enabled by default, false otherwise
	 * @param speed           speed: "fast", "moderate", or "slow"
	 * @param reports         comma separated list of bug pattern codes reported
	 *                         by the detector; empty if unknown
	 * @param requireJRE      string describing JRE version required to run the
	 *                         the detector: e.g., "1.5"
	 */
	public DetectorFactory(Plugin plugin,
			IDetectorCreator detectorCreator,
			boolean enabled ,
			String speed,
			String reports,
			String requireJRE) {
		this.plugin = plugin;
		this.detectorCreator = detectorCreator;
		this.defEnabled = enabled;
		this.speed = speed;
		this.reports = reports;
		this.requireJRE = requireJRE;
		this.priorityAdjustment = 0;
		this.hidden = false;
	}

	/**
	 * Set the overall position in which this detector was specified
	 * in the plugin descriptor.
	 * 
	 * @param positionSpecifiedInPluginDescriptor position in plugin descriptor
	 */
	public void setPositionSpecifiedInPluginDescriptor(
			int positionSpecifiedInPluginDescriptor) {
		this.positionSpecifiedInPluginDescriptor = positionSpecifiedInPluginDescriptor;
	}
	
	/**
	 * Get the overall position in which this detector was specified
	 * in the plugin descriptor.
	 * 
	 * @return position in plugin descriptor
	 */
	public int getPositionSpecifiedInPluginDescriptor() {
		return positionSpecifiedInPluginDescriptor;
	}
	
	/**
	 * Get the Plugin that this Detector is part of.
	 * 
	 * @return the Plugin this Detector is part of
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Determine whether the detector class is a subtype of the given class (or interface).
	 * 
	 * @param otherClass a class or interface
	 * @return true if the detector class is a subtype of the given class or interface
	 */
	public boolean isDetectorClassSubtypeOf(Class<?> otherClass) {
		return otherClass.isAssignableFrom(detectorCreator.getDetectorClass());
	}
	
	/**
	 * Return whether or not this DetectorFactory produces detectors
	 * which report warnings.
	 * 
	 * @return true if the created Detectors report warnings, false if not
	 */
	public boolean isReportingDetector() {
		return !isDetectorClassSubtypeOf(TrainingDetector.class)
			&& !isDetectorClassSubtypeOf(NonReportingDetector.class);

	}
	
	/**
	 * Check to see if we are running on a recent-enough JRE for
	 * this detector to be enabled.
	 * 
	 * @return true if the current JRE is recent enough to run the Detector,
	 *         false if it is too old
	 */
	public boolean isEnabledForCurrentJRE() {
		if (requireJRE.equals(""))
			return true;
		try {
			JavaVersion requiredVersion = new JavaVersion(requireJRE);
			JavaVersion runtimeVersion = JavaVersion.getRuntimeVersion(); 

			if (DEBUG_JAVA_VERSION) {
				System.out.println(
						"Checking JRE version for " + getShortName() +
						" (requires " + requiredVersion +
						", running on " + runtimeVersion + ")");
			}

			
			boolean enabledForCurrentJRE = runtimeVersion.isSameOrNewerThan(requiredVersion);
			if (DEBUG_JAVA_VERSION) {
				System.out.println("\t==> " + enabledForCurrentJRE);
			}
			return enabledForCurrentJRE;
		} catch (JavaVersionException e) {
			if (DEBUG_JAVA_VERSION) {
				System.out.println("Couldn't check Java version: " + e.toString());
				e.printStackTrace(System.out);
			}
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
	public Detector2 create(BugReporter bugReporter) {
		return detectorCreator.createDetector(bugReporter);
	}

	/**
	 * Get the short name of the Detector.
	 * This is the name of the detector class without the package qualification.
	 */
	public String getShortName() {
		String className = detectorCreator.getDetectorClass().getName();
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
		return detectorCreator.getDetectorClass().getName();
	}
}

// vim:ts=4
