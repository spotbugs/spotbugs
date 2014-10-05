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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * A DetectorFactory is responsible for creating instances of Detector objects
 * and for maintaining meta-information about the detector class.
 *
 * @author David Hovemeyer
 * @see Detector
 */
public class DetectorFactory {
    private static final boolean DEBUG_JAVA_VERSION = SystemProperties.getBoolean("findbugs.debug.javaversion");

    // Backwards-compatibility: if the Detector has a setAnalysisContext()
    // method, call it, passing the current AnalysisContext. We do this
    // because some released versions of FindBugs had a Detector
    // interface which specified this method (and ensured it was called
    // before the Detector was used to analyze any code).
    private static final boolean SUPPORT_OLD_DETECTOR_INTERFACE = SystemProperties
            .getBoolean("findbugs.support.old.detector.interface");

    private static final Class<?>[] constructorArgTypes = new Class<?>[] { BugReporter.class };

    static class ReflectionDetectorCreator {
        private final Class<?> detectorClass;

        private Method setAnalysisContext;

        ReflectionDetectorCreator(Class<?> detectorClass) {
            this.detectorClass = detectorClass;
            if (SUPPORT_OLD_DETECTOR_INTERFACE) {
                try {
                    setAnalysisContext = detectorClass.getDeclaredMethod("setAnalysisContext",
                            new Class[] { AnalysisContext.class });
                } catch (NoSuchMethodException e) {
                    // Ignore
                }
            }
        }

        @Override
        public String toString() {
            return detectorClass.getSimpleName();
        }

        public Detector createDetector(BugReporter bugReporter) {
            try {
                Constructor<?> constructor = detectorClass.getConstructor(constructorArgTypes);
                Detector detector = (Detector) constructor.newInstance(new Object[] { bugReporter });
                if (setAnalysisContext != null) {
                    setAnalysisContext.invoke(detector, new Object[] { AnalysisContext.currentAnalysisContext() });
                }
                return detector;
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate " + detectorClass.getName() + " as Detector", e);
            }
        }

        public Detector2 createDetector2(BugReporter bugReporter) {
            if (Detector2.class.isAssignableFrom(detectorClass)) {
                try {
                    Constructor<?> constructor = detectorClass.getConstructor(constructorArgTypes);
                    return (Detector2) constructor.newInstance(new Object[] { bugReporter });
                } catch (Exception e) {
                    throw new RuntimeException("Could not instantiate " + detectorClass.getName() + " as Detector2", e);
                }
            }

            if (Detector.class.isAssignableFrom(detectorClass)) {
                if (NonReportingDetector.class.isAssignableFrom(detectorClass)) {
                    return new NonReportingDetectorToDetector2Adapter(createDetector(bugReporter));
                }
                return new DetectorToDetector2Adapter(createDetector(bugReporter));

            }

            throw new RuntimeException("Class " + detectorClass.getName() + " is not a detector class");
        }

        public Class<?> getDetectorClass() {
            return detectorClass;
        }
    }

    private final @Nonnull Plugin plugin;

    private final ReflectionDetectorCreator detectorCreator;

    private final @Nonnull @DottedClassName String className;

    private int positionSpecifiedInPluginDescriptor;

    private final boolean defEnabled;

    private final String speed;

    private final String reports;

    private final String requireJRE;

    private String detailHTML;

    private int priorityAdjustment;

    private boolean enabledButNonReporting;

    private boolean hidden;

    /**
     * Constructor.
     *
     * @param plugin
     *            the Plugin the Detector is part of
     * @param className
     *            TODO
     * @param detectorClass
     *            the Class object of the Detector
     * @param enabled
     *            true if the Detector is enabled by default, false if disabled
     * @param speed
     *            a string describing roughly how expensive the analysis
     *            performed by the detector is; suggested values are "fast",
     *            "moderate", and "slow"
     * @param reports
     *            comma separated list of bug pattern codes reported by the
     *            detector; empty if unknown
     * @param requireJRE
     *            string describing JRE version required to run the the
     *            detector: e.g., "1.5"
     */
    public DetectorFactory(@Nonnull Plugin plugin, @Nonnull String className,
            Class<?> detectorClass, boolean enabled, String speed,
            String reports, String requireJRE) {
        this.plugin = plugin;
        this.className = className;
        this.detectorCreator = FindBugs.isNoAnalysis() ? null : new ReflectionDetectorCreator(detectorClass);
        this.defEnabled = enabled;
        this.speed = speed;
        this.reports = reports;
        this.requireJRE = requireJRE;
        this.priorityAdjustment = 0;
        this.hidden = false;
    }

    @Override
    public String toString() {
        return getShortName();
    }

    /**
     * Set the overall position in which this detector was specified in the
     * plugin descriptor.
     *
     * @param positionSpecifiedInPluginDescriptor
     *            position in plugin descriptor
     */
    public void setPositionSpecifiedInPluginDescriptor(int positionSpecifiedInPluginDescriptor) {
        this.positionSpecifiedInPluginDescriptor = positionSpecifiedInPluginDescriptor;
    }

    /**
     * Get the overall position in which this detector was specified in the
     * plugin descriptor.
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
     * Determine whether the detector class is a subtype of the given class (or
     * interface).
     *
     * @param otherClass
     *            a class or interface
     * @return true if the detector class is a subtype of the given class or
     *         interface
     */
    public boolean isDetectorClassSubtypeOf(Class<?> otherClass) {
        if (FindBugs.isNoAnalysis()) {
            throw new IllegalStateException("No analysis specified");
        }
        return otherClass.isAssignableFrom(detectorCreator.getDetectorClass());
    }

    /**
     * Return whether or not this DetectorFactory produces detectors which
     * report warnings.
     *
     * @return true if the created Detectors report warnings, false if not
     */
    public boolean isReportingDetector() {
        return !isDetectorClassSubtypeOf(TrainingDetector.class) && !isDetectorClassSubtypeOf(FirstPassDetector.class);
    }


    /**
     * Check to see if we are running on a recent-enough JRE for this detector
     * to be enabled.
     *
     * @return true if the current JRE is recent enough to run the Detector,
     *         false if it is too old
     */
    public boolean isEnabledForCurrentJRE() {
        if ("".equals(requireJRE)) {
            return true;
        }
        try {
            JavaVersion requiredVersion = new JavaVersion(requireJRE);
            JavaVersion runtimeVersion = JavaVersion.getRuntimeVersion();

            if (DEBUG_JAVA_VERSION) {
                System.out.println("Checking JRE version for " + getShortName() + " (requires " + requiredVersion
                        + ", running on " + runtimeVersion + ")");
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
     * Invisible detectors are those that are needed behind the scenes, but
     * shouldn't be explicitly enabled or disabled by the user.
     *
     * @param hidden
     *            true if this factory should be hidden, false if not
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
     * @param priorityAdjustment
     *            the priority adjustment
     */
    public void setPriorityAdjustment(int priorityAdjustment) {
        this.priorityAdjustment = priorityAdjustment;
    }

    public void setEnabledButNonReporting(boolean notReporting) {
        this.enabledButNonReporting = notReporting;
    }

    /**
     * Get the priority adjustment for the detector produced by this factory.
     *
     * @return the priority adjustment
     */
    public int getPriorityAdjustment() {
        if (enabledButNonReporting) {
            return 100;
        }
        return priorityAdjustment;
    }

    /**
     * Get the speed of the Detector produced by this factory.
     */
    @Deprecated
    public String getSpeed() {
        return speed;
    }

    /**
     * Get list of bug pattern codes reported by the detector: empty if unknown.
     */
    public String getReportedBugPatternCodes() {
        return reports;
    }

    /**
     * Get set of all BugPatterns this detector reports. An empty set means that
     * we don't know what kind of bug patterns might be reported.
     */
    public Set<BugPattern> getReportedBugPatterns() {
        Set<BugPattern> result = new TreeSet<BugPattern>();
        StringTokenizer tok = new StringTokenizer(reports, ",");
        while (tok.hasMoreTokens()) {
            String type = tok.nextToken();
            BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(type);
            if (bugPattern != null) {
                result.add(bugPattern);
            }
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
     * Create a Detector instance. This method is only guaranteed to work for
     * old-style detectors using the BCEL bytecode framework.
     *
     * @param bugReporter
     *            the BugReporter to be used to report bugs
     * @return the Detector
     * @deprecated Use createDetector2 in new code
     */
    @Deprecated
    public Detector create(BugReporter bugReporter) {
        if (FindBugs.isNoAnalysis()) {
            throw new IllegalStateException("No analysis specified");
        }
        return detectorCreator.createDetector(bugReporter);
    }

    /**
     * Create a Detector2 instance.
     *
     * @param bugReporter
     *            the BugReporter to be used to report bugs
     * @return the Detector2
     */
    public Detector2 createDetector2(BugReporter bugReporter) {
        if (FindBugs.isNoAnalysis()) {
            throw new IllegalStateException("No analysis specified");
        }
        return detectorCreator.createDetector2(bugReporter);
    }

    /**
     * Get the short name of the Detector. This is the name of the detector
     * class without the package qualification.
     */
    public String getShortName() {
        int endOfPkg = className.lastIndexOf('.');
        if (endOfPkg >= 0) {
            return className.substring(endOfPkg + 1);
        }
        return className;
    }

    /**
     * Get the full name of the detector. This is the name of the detector
     * class, with package qualification.
     */
    public @Nonnull @DottedClassName
    String getFullName() {
        return className;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + className.hashCode();
        result = prime * result + plugin.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DetectorFactory)) {
            return false;
        }
        DetectorFactory other = (DetectorFactory) obj;
        if (!className.equals(other.className)) {
            return false;
        }
        if (!plugin.equals(other.plugin)) {
            return false;
        }
        return true;
    }
}
