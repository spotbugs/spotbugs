/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.ba.AnalysisCacheToAnalysisContextAdapter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassObserver;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.plan.AnalysisPass;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * FindBugs driver class.
 * Experimental version to use the new bytecode-framework-neutral
 * codebase/classpath/classfile infrastructure.
 * Supports all features of the original FindBugs driver.
 * 
 * @author David Hovemeyer
 */
public class FindBugs2 implements IFindBugsEngine {
	private static final boolean VERBOSE = SystemProperties.getBoolean("findbugs.verbose");
	private static final boolean DEBUG = VERBOSE || SystemProperties.getBoolean("findbugs.debug");
	
	private List<IClassObserver> classObserverList;
	private ErrorCountingBugReporter bugReporter;
	private Project project;
	private IClassFactory classFactory;
	private IClassPath classPath;
	private IAnalysisCache analysisCache;
	private List<ClassDescriptor> appClassList;
	private Set<ClassDescriptor> referencedClassSet;
	private DetectorFactoryCollection detectorFactoryCollection;
	private ExecutionPlan executionPlan;
	private UserPreferences userPreferences;
	private String currentClassName;
	private String releaseName;
	private String sourceInfoFileName;
	private AnalysisFeatureSetting[] analysisFeatureSettingList;
	private boolean relaxedReportingMode;
	private String trainingInputDir;
	private String trainingOutputDir;
	private FindBugsProgress progress;
	private IClassScreener classScreener;
	private boolean scanNestedArchives;
	
	/**
	 * Constructor.
	 */
	public FindBugs2() {
		this.classObserverList = new LinkedList<IClassObserver>();
		this.analysisFeatureSettingList = FindBugs.DEFAULT_EFFORT;
		this.progress = new NoOpFindBugsProgress();
		
		// By default, do not exclude any classes via the class screener
		this.classScreener = new IClassScreener() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.IClassScreener#matches(java.lang.String)
			 */
			public boolean matches(String fileName) {
				return true;
			}
		};
		
		// By default, we do want to scan nested archives
		this.scanNestedArchives = true;
	}
	
	/**
	 * Set the detector factory collection to be used by this
	 * FindBugs2 engine.  This method should be called before
	 * the execute() method is called.
	 * 
	 * @param detectorFactoryCollection The detectorFactoryCollection to set.
	 */
	public void setDetectorFactoryCollection(
			DetectorFactoryCollection detectorFactoryCollection) {
		this.detectorFactoryCollection = detectorFactoryCollection;
	}

	/**
	 * Execute the analysis.
	 * For obscure reasons, CheckedAnalysisExceptions are re-thrown
	 * as IOExceptions.  However, these can only happen during the
	 * setup phase where we scan codebases for classes.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void execute() throws IOException, InterruptedException {
		// Get the class factory for creating classpath/codebase/etc. 
		classFactory = ClassFactory.instance();
		
		// The class path object
		createClassPath();
		
		// The analysis cache object
		createAnalysisCache();
		
		progress.reportNumberOfArchives(project.getFileCount());
		
		try {
			// Discover all codebases in classpath and
			// enumerate all classes (application and non-application)
			buildClassPath();
			
			// Build set of classes referenced by application classes
			buildReferencedClassSet();
			
			// Create BCEL compatibility layer
			createAnalysisContext();
			
			// Configure the BugCollection (if we are generating one)
			FindBugs.configureBugCollection(this);

			// Enable/disabled relaxed reporting mode
			FindBugsAnalysisFeatures.setRelaxedMode(relaxedReportingMode);
			
			// Configure training databases
			FindBugs.configureTrainingDatabases(this);
			
			// Configure analysis features
			configureAnalysisFeatures();
			
			// Create the execution plan (which passes/detectors to execute)
			createExecutionPlan();
			
			// Analyze the application
			analyzeApplication();
		} catch (CheckedAnalysisException e) {
			IOException ioe = new IOException("IOException while scanning codebases");
			ioe.initCause(e);
			throw ioe;
		} finally {
			// Make sure the codebases on the classpath are closed
			classPath.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getBugReporter()
	 */
	public BugReporter getBugReporter() {
		return bugReporter;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getProject()
	 */
	public Project getProject() {
		return project;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#addClassObserver(edu.umd.cs.findbugs.classfile.IClassObserver)
	 */
	public void addClassObserver(IClassObserver classObserver) {
		classObserverList.add(classObserver);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#addFilter(java.lang.String, boolean)
	 */
	public void addFilter(String filterFileName, boolean include) throws IOException, FilterException {
		FindBugs.configureFilter(bugReporter, filterFileName, include);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#enableTrainingInput(java.lang.String)
	 */
	public void enableTrainingInput(String trainingInputDir) {
		this.trainingInputDir = trainingInputDir;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#enableTrainingOutput(java.lang.String)
	 */
	public void enableTrainingOutput(String trainingOutputDir) {
		this.trainingOutputDir = trainingOutputDir;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getBugCount()
	 */
	public int getBugCount() {
		return bugReporter.getBugCount();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getCurrentClass()
	 */
	public String getCurrentClass() {
		return currentClassName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getErrorCount()
	 */
	public int getErrorCount() {
		return bugReporter.getErrorCount();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getMissingClassCount()
	 */
	public int getMissingClassCount() {
		return bugReporter.getMissingClassCount();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getReleaseName()
	 */
	public String getReleaseName() {
		return releaseName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setAnalysisFeatureSettings(edu.umd.cs.findbugs.config.AnalysisFeatureSetting[])
	 */
	public void setAnalysisFeatureSettings(AnalysisFeatureSetting[] settingList) {
		this.analysisFeatureSettingList = settingList;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setBugReporter(edu.umd.cs.findbugs.BugReporter)
	 */
	public void setBugReporter(BugReporter bugReporter) {
		this.bugReporter = new ErrorCountingBugReporter(bugReporter);
		addClassObserver(bugReporter);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setClassScreener(edu.umd.cs.findbugs.ClassScreener)
	 */
	public void setClassScreener(IClassScreener classScreener) {
		this.classScreener = classScreener;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setProgressCallback(edu.umd.cs.findbugs.FindBugsProgress)
	 */
	public void setProgressCallback(FindBugsProgress progressCallback) {
		this.progress = progressCallback;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setProject(edu.umd.cs.findbugs.Project)
	 */
	public void setProject(Project project) {
		this.project = project;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setRelaxedReportingMode(boolean)
	 */
	public void setRelaxedReportingMode(boolean relaxedReportingMode) {
		this.relaxedReportingMode = relaxedReportingMode;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setReleaseName(java.lang.String)
	 */
	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setSourceInfoFile(java.lang.String)
	 */
	public void setSourceInfoFile(String sourceInfoFile) {
		this.sourceInfoFileName = sourceInfoFile;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setUserPreferences(edu.umd.cs.findbugs.config.UserPreferences)
	 */
	public void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#emitTrainingOutput()
	 */
	public boolean emitTrainingOutput() {
		return trainingOutputDir != null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getUserPreferences()
	 */
	public UserPreferences getUserPreferences() {
		return userPreferences;
	}

	/**
	 * Create the classpath object.
	 */
	private void createClassPath() {
		classPath = classFactory.createClassPath();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getTrainingInputDir()
	 */
	public String getTrainingInputDir() {
		return trainingInputDir;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getTrainingOutputDir()
	 */
	public String getTrainingOutputDir() {
		return trainingOutputDir;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#useTrainingInput()
	 */
	public boolean useTrainingInput() {
		return trainingInputDir != null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setScanNestedArchives(boolean)
	 */
	public void setScanNestedArchives(boolean scanNestedArchives) {
		this.scanNestedArchives = scanNestedArchives;
	}

	/**
	 * Create the analysis cache object.
	 */
	private void createAnalysisCache() {
		analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);
		
		// TODO: this would be a good place to load "analysis plugins" which could
		// add additional analysis engines.  Or, perhaps when we load
		// detector plugins we should check for analysis engines.
		// Either way, allowing plugins to add new analyses would be nice.
		new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.bcel.EngineRegistrar().registerAnalysisEngines(analysisCache);
		
		Global.setAnalysisCacheForCurrentThread(analysisCache);
	}

	/**
	 * Build the classpath from project codebases and system codebases.
	 * 
	 * @throws InterruptedException if the analysis thread is interrupted
	 * @throws IOException if an I/O error occurs
	 * @throws ResourceNotFoundException 
	 */
	private void buildClassPath() throws InterruptedException, IOException, CheckedAnalysisException {
		IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);
		
		for (String path : project.getFileArray()) {
			builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), true);
		}
		for (String path : project.getAuxClasspathEntryList()) {
			builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), false);
		}
		
		builder.scanNestedArchives(scanNestedArchives);
		
		builder.build(classPath, progress);
		
		appClassList = builder.getAppClassList();
		
		// If any of the application codebases contain source code,
		// add them to the source path.
		// Also, use the last modified time of application codebases
		// to set the project timestamp.
		for (Iterator<? extends ICodeBase> i = classPath.appCodeBaseIterator(); i.hasNext(); ){
			ICodeBase appCodeBase = i.next();
			
			if (appCodeBase.containsSourceFiles()) {
				String pathName = appCodeBase.getPathName();
				if (pathName != null) {
					project.addSourceDir(pathName);
				}
			}
			
			project.addTimestamp(appCodeBase.getLastModifiedTime());
		}
		
	}
	
	private void buildReferencedClassSet() throws CheckedAnalysisException, InterruptedException {
		// XXX: should drive progress dialog (scanning phase)?
		
		referencedClassSet = new TreeSet<ClassDescriptor>();
		
		LinkedList<ClassDescriptor> workList = new LinkedList<ClassDescriptor>();
		workList.addAll(appClassList);

		Set<ClassDescriptor> seen = new HashSet<ClassDescriptor>();
		Set<ClassDescriptor> appClassSet = new HashSet<ClassDescriptor>(appClassList);
		
		Set<ClassDescriptor> badAppClassSet = new HashSet<ClassDescriptor>();
		
		while (!workList.isEmpty()) {
			if (Thread.interrupted())
				throw new InterruptedException();
			ClassDescriptor classDesc = workList.removeFirst();
			
			if (seen.contains(classDesc)) {
				continue;
			}
			seen.add(classDesc);
			
			referencedClassSet.add(classDesc);
			
			// Get list of referenced classes and add them to set.
			// Add superclasses and superinterfaces to worklist.
			try {
				ClassInfo classInfo = Global.getAnalysisCache().getClassAnalysis(ClassInfo.class, classDesc);
				referencedClassSet.addAll(Arrays.asList(classInfo.getReferencedClassDescriptorList()));
				
				if (classInfo.getSuperclassDescriptor() != null) {
					workList.addLast(classInfo.getSuperclassDescriptor());
				}
				
				for (ClassDescriptor ifaceDesc : classInfo.getInterfaceDescriptorList()) {
					workList.addLast(ifaceDesc);
				}
			} catch (MissingClassException e) {
				// Just log it as a missing class
				bugReporter.reportMissingClass(e.getClassDescriptor());
				if (appClassSet.contains(classDesc)) {
					badAppClassSet.add(classDesc);
				}
			} catch (CheckedAnalysisException e) {
				// Failed to scan a referenced class --- just log the error and continue
				bugReporter.logError("Error scanning " + classDesc + " for referenced classes", e);
				if (appClassSet.contains(classDesc)) {
					badAppClassSet.add(classDesc);
				}
			}
		}
		
		// Delete any application classes that could not be read
		appClassList.removeAll(badAppClassSet);
	}
	
	/**
	 * Create the AnalysisContext that will serve as the BCEL-compatibility
	 * layer over the AnalysisCache.
	 */
	private void createAnalysisContext() throws CheckedAnalysisException, IOException {
		AnalysisCacheToAnalysisContextAdapter analysisContext =
			new AnalysisCacheToAnalysisContextAdapter();
		
		// Make the AnalysisCache the backing store for
		// the BCEL Repository
		analysisContext.clearRepository();

		// Specify which classes are application classes
		analysisContext.setAppClassList(appClassList);
		
		// If needed, load SourceInfoMap
		if (sourceInfoFileName != null) {
			SourceInfoMap sourceInfoMap = analysisContext.getSourceInfoMap();
			sourceInfoMap.read(new FileInputStream(sourceInfoFileName));
		}

		// Make this the current analysis context
		AnalysisContext.setCurrentAnalysisContext(analysisContext);
	}
	
	/**
	 * Configure analysis feature settings.
	 */
	private void configureAnalysisFeatures() {
		for (AnalysisFeatureSetting setting : analysisFeatureSettingList) {
			setting.configure(AnalysisContext.currentAnalysisContext());
		}
	}

	/**
	 * Create an execution plan.
	 * 
	 * @throws OrderingConstraintException if the detector ordering constraints are inconsistent
	 */
	private void createExecutionPlan() throws OrderingConstraintException {
		executionPlan = new ExecutionPlan();
		
		// Use user preferences to decide which detectors are enabled.
		DetectorFactoryChooser detectorFactoryChooser = new DetectorFactoryChooser() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.DetectorFactoryChooser#choose(edu.umd.cs.findbugs.DetectorFactory)
			 */
			public boolean choose(DetectorFactory factory) {
				return FindBugs.isDetectorEnabled(FindBugs2.this, factory);
			}
		};
		executionPlan.setDetectorFactoryChooser(detectorFactoryChooser);
		
		// Add plugins
		for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			if (DEBUG) {
				System.out.println("Adding plugin " + plugin.getPluginId() + " to execution plan");
			}
			executionPlan.addPlugin(plugin);
		}
		
		// Build the execution plan
		executionPlan.build();
		
		if (DEBUG) {
			System.out.println(executionPlan.getNumPasses() + " passes in execution plan");
		}
	}

	/**
	 * Analyze the classes in the application codebase.
	 */
	private void analyzeApplication() throws InterruptedException {
		int passCount = 0;
		boolean multiplePasses = executionPlan.getNumPasses() > 1;
		
		for (Iterator<AnalysisPass> i = executionPlan.passIterator(); i.hasNext(); ) {
			AnalysisPass pass = i.next();

			// Instantiate the detectors
			Detector2[] detectorList = pass.instantiateDetector2sInPass(bugReporter);

			// If there are multiple passes, then on the first pass,
			// we apply detectors to all classes referenced by the application classes.
			// On subsequent passes, we apply detector only to application classes.
			Collection<ClassDescriptor> classCollection = (multiplePasses && passCount == 0)
					? referencedClassSet 
					: appClassList;
			if (DEBUG) {
				System.out.println("Pass " + (passCount) + ": " + classCollection.size() + " classes");
			}
			
			progress.startAnalysis(classCollection.size());
			
			for (ClassDescriptor classDescriptor : classCollection) {
				if (DEBUG) {
					System.out.println("Class " + classDescriptor);
				}
				
				if (!classScreener.matches(classDescriptor.toResourceName())) {
					if (DEBUG) {
						System.out.println("*** Excluded by class screener");
					}
					continue;
				}
				
				currentClassName = ClassName.toDottedClassName(classDescriptor.getClassName());
				notifyClassObservers(classDescriptor);
				
				for (Detector2 detector : detectorList) {
					if (Thread.interrupted())
						throw new InterruptedException();
					if (DEBUG) {
						System.out.println("Applying " + detector.getDetectorClassName() + " to " + classDescriptor);
					}
					try {
						detector.visitClass(classDescriptor);
					} catch (MissingClassException e) {
						Global.getAnalysisCache().getErrorLogger().reportMissingClass(e.getClassDescriptor());
					} catch (CheckedAnalysisException e) {
						logRecoverableException(classDescriptor, detector, e);
					} catch (AnalysisException e) {
						logRecoverableException(classDescriptor, detector, e);
					} catch (ArrayIndexOutOfBoundsException e) {
						logRecoverableException(classDescriptor, detector, e);
					} catch (ClassCastException e) {
						logRecoverableException(classDescriptor, detector, e);
					}
				}
				
				progress.finishClass();
			}
			
			// Call finishPass on each detector
			for (Detector2 detector : detectorList) {
				detector.finishPass();
			}
			
			AnalysisContext.currentAnalysisContext().updateDatabases(passCount);
			progress.finishPerClassAnalysis();
			
			passCount++;
		}

		// Flush any queued bug reports
		bugReporter.finish();

		// Flush any queued error reports
		bugReporter.reportQueuedErrors();
	}

	/**
	 * Notify all IClassObservers that we are visiting given class.
	 * 
	 * @param classDescriptor the class being visited
	 */
	private void notifyClassObservers(ClassDescriptor classDescriptor) {
		for (IClassObserver observer : classObserverList) {
			observer.observeClass(classDescriptor);
		}
	}

	/**
	 * Report an exception that occurred while analyzing a class
	 * with a detector.
	 * 
	 * @param classDescriptor class being analyzed
	 * @param detector        detector doing the analysis
	 * @param e               the exception
	 */
	private void logRecoverableException(
			ClassDescriptor classDescriptor, Detector2 detector, Throwable e) {
		bugReporter.logError("Exception analyzing " + classDescriptor.toDottedClassName() +
				" using detector " + detector.getDetectorClassName(), e);
	}

	public static void main(String[] args) throws Exception {
		// Create FindBugs2 engine
		FindBugs2 findBugs = new FindBugs2();
		
		// Parse command line and configure the engine
		TextUICommandLine commandLine = new TextUICommandLine();
		FindBugs.processCommandLine(commandLine, args, findBugs);
		
		// Away we go!
		FindBugs.runMain(findBugs, commandLine);
	}
}
