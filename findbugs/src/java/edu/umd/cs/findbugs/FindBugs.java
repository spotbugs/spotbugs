/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.config.CommandLine.HelpRequestedException;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.FilterException;

/**
 * Static methods and fields useful for working with instances of
 * IFindBugsEngine.
 * 
 * This class was previously the main driver for FindBugs analyses,
 * but has been replaced by {@link FindBugs2 FindBugs2}.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public abstract class FindBugs  {
	/**
	 * Analysis settings for -effort:min.
	 */
	public static final AnalysisFeatureSetting[] MIN_EFFORT = new AnalysisFeatureSetting[]{
		new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, true),
		new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, false),
		new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, false),
		new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, false),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, false),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, false),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, false),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false),
	};

	/**
	 * Analysis settings for -effort:less.
	 */
	public static final AnalysisFeatureSetting[] LESS_EFFORT = new AnalysisFeatureSetting[]{
		new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
		new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
		new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, false),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, false),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, false),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false),
	};
	
	/**
	 * Analysis settings for -effort:default.
	 */
	public static final AnalysisFeatureSetting[] DEFAULT_EFFORT = new AnalysisFeatureSetting[]{
		new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
		new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
		new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false),
	};
	
	/**
	 * Analysis settings for -effort:more.
	 */
	public static final AnalysisFeatureSetting[] MORE_EFFORT = new AnalysisFeatureSetting[]{
		new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
		new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
		new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false),
	};
	
	/**
	 * Analysis settings for -effort:max.
	 */
	public static final AnalysisFeatureSetting[] MAX_EFFORT = new AnalysisFeatureSetting[]{
		new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
		new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
		new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, false),
		new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
		new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, true),
	};

	/**
	 * Debug tracing.
	 */
	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.debug");

	// The following don't seem to be used...
//	public static final boolean TIMEDEBUG = SystemProperties.getBoolean("findbugs.time");
//	public static final int TIMEQUANTUM = SystemProperties.getInteger("findbugs.time.quantum", 1000);

	/**
	 * FindBugs home directory.
	 */
	private static String home = SystemProperties.getProperty("findbugs.home");

	/**
	 * Known URL protocols.
	 * Filename URLs that do not have an explicit protocol are
	 * assumed to be files.
	 */
	static public final Set<String> knownURLProtocolSet = new HashSet<String>();
	static {
		knownURLProtocolSet.add("file");
		knownURLProtocolSet.add("http");
		knownURLProtocolSet.add("https");
		knownURLProtocolSet.add("jar");
	}

	/**
	 * Set the FindBugs home directory.
	 */
	public static void setHome(String home) {
		FindBugs.home = home;
	}

	/**
	 * Get the FindBugs home directory.
	 */
	public static String getHome() {
		return home;
	}

	/**
	 * Configure training databases.
	 * 
	 * @param findBugs the IFindBugsEngine to configure
	 * @throws IOException
	 */
	public static void configureTrainingDatabases(IFindBugsEngine findBugs) throws IOException {
		if (findBugs.emitTrainingOutput()) {
			String trainingOutputDir = findBugs.getTrainingOutputDir();

			if (!new File(trainingOutputDir).isDirectory())
				throw new IOException("Training output directory " + trainingOutputDir + " does not exist");
			AnalysisContext.currentAnalysisContext().setDatabaseOutputDir(trainingOutputDir);
			// XXX: hack
			System.setProperty("findbugs.checkreturn.savetraining", new File(trainingOutputDir, "checkReturn.db").getPath());
		}
		if (findBugs.useTrainingInput()) {
			String trainingInputDir = findBugs.getTrainingInputDir();

			if (!new File(trainingInputDir).isDirectory())
				throw new IOException("Training input directory " + trainingInputDir + " does not exist");
			AnalysisContext.currentAnalysisContext().setDatabaseInputDir(trainingInputDir);
			AnalysisContext.currentAnalysisContext().loadInterproceduralDatabases();
			// XXX: hack
			System.setProperty("findbugs.checkreturn.loadtraining", new File(trainingInputDir, "checkReturn.db").getPath());
		}
		else {
			AnalysisContext.currentAnalysisContext().loadDefaultInterproceduralDatabases();
		}
	}

	/**
	 * Determing whether or not given DetectorFactory should be enabled.
	 * 
	 * @param findBugs the IFindBugsEngine
	 * @param factory the DetectorFactory
	 * @return true if the DetectorFactory should be enabled, false otherwise
	 */
	public static boolean isDetectorEnabled(IFindBugsEngine findBugs, DetectorFactory factory) {
		if (!factory.getPlugin().isEnabled())
			return false;

		if (!findBugs.getUserPreferences().isDetectorEnabled(factory))
			return false;

		if (!factory.isEnabledForCurrentJRE())
			return false;

		// Slow first pass detectors are usually disabled, but may be explicitly enabled
		if (!AnalysisContext.currentAnalysisContext().getBoolProperty(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS)
				&& factory.isDetectorClassSubtypeOf(InterproceduralFirstPassDetector.class))
			return false;

		// Training detectors are enabled if, and only if, we are emitting training output
		boolean isTrainingDetector = factory.isDetectorClassSubtypeOf(TrainingDetector.class);
		boolean isNonReportingDetector = factory.isDetectorClassSubtypeOf(NonReportingDetector.class);
		if (findBugs.emitTrainingOutput()) {
			return isTrainingDetector || isNonReportingDetector;
		}

		if (isTrainingDetector) return false;

		return true;
	}

	/**
	 * Process -bugCategories option.
	 * 
	 * @param userPreferences
	 *            UserPreferences representing which Detectors are enabled
	 * @param categories
	 *            comma-separated list of bug categories
	 * @return Set of categories to be used
	 */
	public static Set<String> handleBugCategories(UserPreferences userPreferences, String categories) {
		// Parse list of bug categories
		Set<String> categorySet = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(categories, ",");
		while (tok.hasMoreTokens()) {
			categorySet.add(tok.nextToken());
		}

		return categorySet;
	}
	
	/**
	 * Process the command line.
	 * 
	 * @param commandLine  the TextUICommandLine object which will parse the command line
	 * @param argv         the command line arguments
	 * @param findBugs     the IFindBugsEngine to configure
	 * @throws IOException
	 * @throws FilterException
	 */
	public static void processCommandLine(TextUICommandLine commandLine, String[] argv, IFindBugsEngine findBugs) throws IOException, FilterException {
		// Expand option files in command line.
		// An argument beginning with "@" is treated as specifying
		// the name of an option file.
		// Each line of option files are treated as a single argument.
		// Blank lines and comment lines (beginning with "#")
		// are ignored.
		argv = CommandLine.expandOptionFiles(argv, true, true);

		int argCount = 0;
		try {
			argCount = commandLine.parse(argv);
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			showHelp(commandLine);
		} catch (HelpRequestedException e) {
			showHelp(commandLine);
		}

		Project project = commandLine.getProject();
		for (int i = argCount; i < argv.length; ++i)
			project.addFile(argv[i]);
		commandLine.handleXArgs();

		if (project.getFileCount() == 0) {
			showHelp(commandLine);
		}

		commandLine.configureEngine(findBugs);
	}

	/**
	 * Show -help message.
	 * 
	 * @param commandLine
	 */
	@SuppressWarnings("DM_EXIT")
	public static void showHelp(TextUICommandLine commandLine) {
		showSynopsis();
		ShowHelp.showGeneralOptions();
		FindBugs.showCommandLineOptions(commandLine);
		System.exit(1);
	}

	/**
	 * Given a fully-configured IFindBugsEngine and the TextUICommandLine
	 * used to configure it, execute the analysis.
	 * 
	 * @param findBugs    a fully-configured IFindBugsEngine
	 * @param commandLine the TextUICommandLine used to configure the IFindBugsEngine
	 * @throws java.io.IOException
	 * @throws java.lang.RuntimeException
	 */
	@SuppressWarnings("DM_EXIT")
	public static void runMain(IFindBugsEngine findBugs, TextUICommandLine commandLine)
			throws java.io.IOException, RuntimeException {
		try {
			findBugs.execute();
		} catch (InterruptedException e) {
			// Not possible when running from the command line
		}

		int bugCount = findBugs.getBugCount();
		int missingClassCount = findBugs.getMissingClassCount();
		int errorCount = findBugs.getErrorCount();

		if (!commandLine.quiet() || commandLine.setExitCode()) {
			if (bugCount > 0)
				System.err.println("Warnings generated: " + bugCount);
			if (missingClassCount > 0)
				System.err.println("Missing classes: " + missingClassCount);
			if (errorCount > 0)
				System.err.println("Analysis errors: " + errorCount);
		}

		if (commandLine.setExitCode()) {
			int exitCode = 0;
			if (errorCount > 0)
				exitCode |= ExitCodes.ERROR_FLAG;
			if (missingClassCount > 0)
				exitCode |= ExitCodes.MISSING_CLASS_FLAG;
			if (bugCount > 0)
				exitCode |= ExitCodes.BUGS_FOUND_FLAG;

			System.exit(exitCode);
		}
	}

	/**
	 * Print command line options synopses to stdout.
	 */
	public static void showCommandLineOptions() {
		showCommandLineOptions(new TextUICommandLine());
	}

	/**
	 * Print command line options synopses to stdout.
	 * 
	 * @param commandLine the TextUICommandLine whose options should be printed
	 */
	public static void showCommandLineOptions(TextUICommandLine commandLine) {
		System.out.println("Command line options:");
		commandLine.printUsage(System.out);
	}

	/**
	 * Show the overall FindBugs command synopsis.
	 */
	public static void showSynopsis() {
		System.out.println("Usage: findbugs [general options] -textui [command line options...] [jar/zip/class files, directories...]");
	}

	/**
	 * Configure the (bug instance) Filter for the given DelegatingBugReporter.
	 * 
	 * @param bugReporter     a DelegatingBugReporter
	 * @param filterFileName  filter file name
	 * @param include         true if the filter is an include filter, false if it's an exclude filter
	 * @throws java.io.IOException
	 * @throws edu.umd.cs.findbugs.filter.FilterException
	 */
	public static void configureFilter(DelegatingBugReporter bugReporter, String filterFileName, boolean include)
			throws IOException, FilterException {
		Filter filter = new Filter(filterFileName);
		BugReporter origBugReporter = bugReporter.getDelegate();
		BugReporter filterBugReporter = new FilterBugReporter(origBugReporter, filter, include);
		bugReporter.setDelegate(filterBugReporter);
	}
	
	/**
	 * Configure a baseline bug instance filter.
	 * 
	 * @param bugReporter        a DelegatingBugReporter
	 * @param baselineFileName   filename of baseline Filter
	 * @throws java.io.IOException
	 * @throws org.dom4j.DocumentException
	 */
	public static void configureBaselineFilter(DelegatingBugReporter bugReporter, String baselineFileName)
			throws IOException, DocumentException  {
		BugReporter origBugReporter = bugReporter.getDelegate();
		BugReporter filterBugReporter = new ExcludingHashesBugReporter(origBugReporter, baselineFileName);
		bugReporter.setDelegate(filterBugReporter);
	}
	
	/**
	 * Configure the BugCollection (if the BugReporter being used
	 * is constructing one).
	 * 
	 * @param findBugs the IFindBugsEngine
	 */
	public static void configureBugCollection(IFindBugsEngine findBugs) {
		BugReporter realBugReporter = findBugs.getBugReporter().getRealBugReporter();

		if (realBugReporter instanceof BugCollectionBugReporter) {
			BugCollectionBugReporter bugCollectionBugReporter =
				(BugCollectionBugReporter) realBugReporter;

			bugCollectionBugReporter = (BugCollectionBugReporter) realBugReporter;

			bugCollectionBugReporter.getBugCollection().setReleaseName(findBugs.getReleaseName());

			Project project = findBugs.getProject();

			if (project.getProjectName() == null)
				project.setProjectName(findBugs.getProjectName());
			if (project.getTimestamp() != 0) {
				bugCollectionBugReporter.getBugCollection().setTimestamp(project.getTimestamp());
				bugCollectionBugReporter.getBugCollection().getProjectStats().setTimestamp(project.getTimestamp());
			}

		}
	}
}

// vim:ts=4
