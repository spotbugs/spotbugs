/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2008 University of Maryland
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.ClassFormatException;
import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.ba.AnalysisCacheToAnalysisContextAdapter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassObserver;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.plan.AnalysisPass;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.util.TopologicalSort.OutEdges;
import java.util.Map;

/**
 * FindBugs driver class.
 * Orchestrates the analysis of a project, collection of results, etc.
 *
 * @author David Hovemeyer
 */
public class FindBugs2 implements IFindBugsEngine2 {
	private static final boolean LIST_ORDER = SystemProperties.getBoolean("findbugs.listOrder");

	private static final boolean VERBOSE = SystemProperties.getBoolean("findbugs.verbose");
	public static final boolean DEBUG = VERBOSE || SystemProperties.getBoolean("findbugs.debug");
	private static final boolean SCREEN_FIRST_PASS_CLASSES = SystemProperties.getBoolean("findbugs.screenFirstPass");
	
	private static final boolean DEBUG_UA = SystemProperties.getBoolean("ua.debug");

	private List<IClassObserver> classObserverList;
	private ErrorCountingBugReporter bugReporter;
	private Project project;
	private IClassFactory classFactory;
	private IClassPath classPath;
	private IAnalysisCache analysisCache;
	private List<ClassDescriptor> appClassList;
	private Collection<ClassDescriptor> referencedClassSet;
	private DetectorFactoryCollection detectorFactoryCollection;
	private ExecutionPlan executionPlan;
	private UserPreferences userPreferences;
	private String currentClassName;
	private String releaseName;
	private String projectName;
	private String sourceInfoFileName;
	private AnalysisFeatureSetting[] analysisFeatureSettingList;
	private boolean relaxedReportingMode;
	private boolean abridgedMessages;
	private String trainingInputDir;
	private String trainingOutputDir;
	private FindBugsProgress progress;
	private IClassScreener classScreener;
	private boolean scanNestedArchives;
	private boolean noClassOk;
	private edu.umd.cs.findbugs.userAnnotations.Plugin userAnnotationPlugin;
	private boolean userAnnotationSync;

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

			public boolean vacuous() {
	            return true;
            }
		};

		// By default, we do not want to scan nested archives
		this.scanNestedArchives = false;
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
		Profiler profiler = Profiler.getInstance();

		try {
			// Get the class factory for creating classpath/codebase/etc.
			classFactory = ClassFactory.instance();

			// The class path object
			createClassPath();

			// The analysis cache object
			createAnalysisCache();

			progress.reportNumberOfArchives(project.getFileCount() + project.getNumAuxClasspathEntries());
			profiler.start(this.getClass());

			// Discover all codebases in classpath and
			// enumerate all classes (application and non-application)
			buildClassPath();

			// Build set of classes referenced by application classes
			buildReferencedClassSet();

			// Create BCEL compatibility layer
			createAnalysisContext(project, appClassList, sourceInfoFileName);

			// Configure the BugCollection (if we are generating one)
			FindBugs.configureBugCollection(this);

			// Enable/disabled relaxed reporting mode
			FindBugsAnalysisFeatures.setRelaxedMode(relaxedReportingMode);
			FindBugsDisplayFeatures.setAbridgedMessages(abridgedMessages);

			// Configure training databases
			FindBugs.configureTrainingDatabases(this);

			// Configure analysis features
			configureAnalysisFeatures();

			// Create the execution plan (which passes/detectors to execute)
			createExecutionPlan();

			if (!classScreener.vacuous()) {
				final BugReporter origBugReporter = bugReporter.getDelegate();
				BugReporter filterBugReporter = new DelegatingBugReporter(origBugReporter) {

					@Override
					public void reportBug(BugInstance bugInstance) {
						String className = bugInstance.getPrimaryClass().getClassName();
						String resourceName = className.replace('.', '/') + ".class";
						if (classScreener.matches(resourceName)) {
							this.getDelegate().reportBug(bugInstance);
						}
					}
				};
				bugReporter.setDelegate(filterBugReporter);
			}

			if (appClassList.size() == 0) {
				if (noClassOk) {
					System.err.println("No classfiles specified; output will have no warnings");
				} else {
					throw new NoClassesFoundToAnalyzeException(classPath);
				}
			}

			// Analyze the application
			analyzeApplication();
		} catch (CheckedAnalysisException e) {
			IOException ioe = new IOException("IOException while scanning codebases");
			ioe.initCause(e);
			throw ioe;
		} catch (OutOfMemoryError e) {
			System.err.println("Out of memory");
			System.err.println("Total memory: " + Runtime.getRuntime().maxMemory() / 1000000 + "M");
			System.err.println(" free memory: " + Runtime.getRuntime().freeMemory() / 1000000 + "M");

			for (String s : project.getFileList()) {
				System.err.println("Analyzed: " + s);
			}
			for (String s : project.getAuxClasspathEntryList()) {
				System.err.println("     Aux: " + s);
			}
			throw e;
		} finally {
			AnalysisContext.removeCurrentAnalysisContext();
			Global.removeAnalysisCacheForCurrentThread();
			DescriptorFactory.clearInstance();
			ObjectTypeFactory.clearInstance();
			TypeQualifierApplications.clearInstance();
			TypeQualifierAnnotation.clearInstance();
			TypeQualifierValue.clearInstance();
			// Make sure the codebases on the classpath are closed
			if (classPath != null) {
				classPath.close();
			}
			profiler.end(this.getClass());
			profiler.report();
		}
	}

	/**
	 * To avoid cyclic cross-references and allow GC after engine is not more needed.
	 * (used by Eclipse plugin)
	 */
	public void dispose() {
		if (executionPlan != null) executionPlan.dispose();
		if (appClassList != null) appClassList.clear();
		if (classObserverList != null)  classObserverList.clear();
		if (referencedClassSet != null) referencedClassSet.clear();
		analysisCache = null;
	    analysisFeatureSettingList = null;
	    bugReporter = null;
	    classFactory = null;
	    classPath = null;
	    classScreener = null;
	    detectorFactoryCollection = null;
	    executionPlan = null;
	    progress = null;
	    project = null;
	    userPreferences = null;
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
     * @see edu.umd.cs.findbugs.IFindBugsEngine#addBaselineBugs(java.lang.String)
     */
    public void excludeBaselineBugs(String baselineBugs) throws IOException, DocumentException {
    		FindBugs.configureBaselineFilter(bugReporter, baselineBugs);
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

	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String name) {
		projectName = name;
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setNoClassOk(boolean)
	 */
	public void setNoClassOk(boolean noClassOk) {
		this.noClassOk = noClassOk;
	}

	public void loadUserAnnotationPlugin(String userAnnotationPluginClassName, Map<String,String> configurationProperties)
			throws IOException {
		if (DEBUG_UA) {
			System.out.println("Attempting to load user annotation plugin " + userAnnotationPluginClassName + "...");
		}
		
		try {
			// FIXME: need to think of how to specify what codebase
			// the user annotation plugin should be loaded from.
			Class<?> cls = Class.forName(userAnnotationPluginClassName);
			if (!edu.umd.cs.findbugs.userAnnotations.Plugin.class.isAssignableFrom(cls)) {
				throw new IOException("Class " + userAnnotationPluginClassName + " is not a user annotation plugin");
			}

			Object instance = cls.newInstance();
			
			this.userAnnotationPlugin =
				edu.umd.cs.findbugs.userAnnotations.Plugin.class.cast(instance);
			
			this.userAnnotationPlugin.setProperties(configurationProperties);
			
			if (DEBUG_UA) {
				System.out.println("  Successfully loaded and configured " + userAnnotationPluginClassName);
			}

		} catch (ClassNotFoundException e) {
			Util.throwIOException("Could not load user annotation plugin", e);
		} catch (InstantiationException e) {
			Util.throwIOException("Could not create instance of user annotation plugin object", e);
		} catch (IllegalAccessException e) {
			Util.throwIOException("Could not create instance of user annotation plugin object", e);
		}
	}

	public void setUserAnnotationSync(boolean userAnnotationSync) {
		if (DEBUG_UA) {
			System.out.println("Will synchronize user annotations");
		}
		this.userAnnotationSync = userAnnotationSync;
	}

	/**
	 * Create the analysis cache object.
	 *
	 * @throws IOException if error occurs registering analysis engines in a plugin
	 */
	private void createAnalysisCache() throws IOException {
		analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);

		// Register the "built-in" analysis engines
		registerBuiltInAnalysisEngines(analysisCache);

		// Register analysis engines in plugins
		registerPluginAnalysisEngines(detectorFactoryCollection, analysisCache);

		// Install the DetectorFactoryCollection as a database
		analysisCache.eagerlyPutDatabase(DetectorFactoryCollection.class, detectorFactoryCollection);

		Global.setAnalysisCacheForCurrentThread(analysisCache);
	}

	/**
	 * Register the "built-in" analysis engines with given IAnalysisCache.
	 *
	 * @param analysisCache an IAnalysisCache
     */
    public static void registerBuiltInAnalysisEngines(IAnalysisCache analysisCache) {
	    new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.bcel.EngineRegistrar().registerAnalysisEngines(analysisCache);
    }

	/**
	 * Register all of the analysis engines defined in the plugins
	 * contained in a DetectorFactoryCollection with an IAnalysisCache.
	 *
	 * @param detectorFactoryCollection a DetectorFactoryCollection
	 * @param analysisCache             an IAnalysisCache
     * @throws IOException
     */
    public static void registerPluginAnalysisEngines(
    		DetectorFactoryCollection detectorFactoryCollection, IAnalysisCache analysisCache) throws IOException {
	    for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext(); ) {
			Plugin plugin = i.next();

			Class<? extends IAnalysisEngineRegistrar> engineRegistrarClass =
				plugin.getEngineRegistrarClass();
			if (engineRegistrarClass != null) {
				try {
	                IAnalysisEngineRegistrar engineRegistrar = engineRegistrarClass.newInstance();
	                engineRegistrar.registerAnalysisEngines(analysisCache);
                } catch (InstantiationException e) {
                	IOException ioe = new IOException(
                			"Could not create analysis engine registrar for plugin " + plugin.getPluginId());
                	ioe.initCause(e);
                	throw ioe;
                } catch (IllegalAccessException e) {
                	IOException ioe = new IOException(
                			"Could not create analysis engine registrar for plugin " + plugin.getPluginId());
                	ioe.initCause(e);
                	throw ioe;
                }
			}
		}
    }

	/**
	 * Build the classpath from project codebases and system codebases.
	 *
	 * @throws InterruptedException if the analysis thread is interrupted
	 * @throws IOException if an I/O error occurs
	 * @throws CheckedAnalysisException
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

		if (DEBUG) {
	        System.out.println(appClassList.size() + " classes scanned");
        }

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

		if (DEBUG) {
	        System.out.println("Adding referenced classes");
        }
		Set<String> referencedPackageSet = new HashSet<String>();

		LinkedList<ClassDescriptor> workList = new LinkedList<ClassDescriptor>();
		workList.addAll(appClassList);

		Set<ClassDescriptor> seen = new HashSet<ClassDescriptor>();
		Set<ClassDescriptor> appClassSet = new HashSet<ClassDescriptor>(appClassList);

		Set<ClassDescriptor> badAppClassSet = new HashSet<ClassDescriptor>();
		HashSet<ClassDescriptor> knownDescriptors = new HashSet<ClassDescriptor>(DescriptorFactory.instance().getAllClassDescriptors());
		int count = 0;
		while (!workList.isEmpty()) {
			if (Thread.interrupted()) {
	            throw new InterruptedException();
            }
			ClassDescriptor classDesc = workList.removeFirst();

			if (seen.contains(classDesc)) {
				continue;
			}
			seen.add(classDesc);


			if (!knownDescriptors.contains(classDesc)) {
				count++;
				if (DEBUG && count % 5000 == 0) {
					System.out.println("Adding referenced class " + classDesc);
				}
			}

			referencedPackageSet.add(classDesc.getPackageName());

			// Get list of referenced classes and add them to set.
			// Add superclasses and superinterfaces to worklist.
			try {
				XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);

				if (classNameAndInfo.getSuperclassDescriptor() != null) {
					workList.addLast(classNameAndInfo.getSuperclassDescriptor());
				}

				for (ClassDescriptor ifaceDesc : classNameAndInfo.getInterfaceDescriptorList()) {
					workList.addLast(ifaceDesc);
				}
			} catch (RuntimeException e) {
				bugReporter.logError("Error scanning " + classDesc + " for referenced classes", e);
				if (appClassSet.contains(classDesc)) {
					badAppClassSet.add(classDesc);
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
		DescriptorFactory.instance().purge(badAppClassSet);

		for(ClassDescriptor d : DescriptorFactory.instance().getAllClassDescriptors()) {
	        referencedPackageSet.add(d.getPackageName());
        }

		// Based on referenced packages, add any resolvable package-info classes
		// to the set of referenced classes.
		if (DEBUG) {
		referencedPackageSet.remove("");
		System.out.println("Added " + count + " referenced classes");
		System.out.println("Total of " + referencedPackageSet.size() + " packages");
		}

		// TODO the block below seems to be an old workaround which does not add any value
		// except even more "package-info not found" exceptions
		if(Boolean.getBoolean("fb.addPackageInfo"))
		for (String pkg : referencedPackageSet) {
			ClassDescriptor pkgInfoDesc = DescriptorFactory.instance().getClassDescriptorForDottedClassName(pkg + ".package-info");
			if (DEBUG) {
				System.out.println("Checking package " + pkg + " for package-info...");
			}
			try {
				analysisCache.getClassAnalysis(ClassData.class, pkgInfoDesc); // check that data is there
				analysisCache.getClassAnalysis(XClass.class, pkgInfoDesc);
				if (DEBUG) {
					System.out.println("   Adding " + pkgInfoDesc + " to referenced classes");
				}
			} catch (CheckedAnalysisException e) {
				// Ignore
			}
		}
		referencedClassSet = new ArrayList<ClassDescriptor>(DescriptorFactory.instance().getAllClassDescriptors());
	}

	 public List<ClassDescriptor> sortByCallGraph(Collection<ClassDescriptor> classList, OutEdges<ClassDescriptor> outEdges) {
		List<ClassDescriptor> evaluationOrder = edu.umd.cs.findbugs.util.TopologicalSort.sortByCallGraph(classList, outEdges);
		edu.umd.cs.findbugs.util.TopologicalSort.countBadEdges(evaluationOrder, outEdges);
		return evaluationOrder;

		}


	 public static void clearAnalysisContext() {
		 AnalysisContext.removeCurrentAnalysisContext();

	 }
	/**
	 * Create the AnalysisContext that will serve as the BCEL-compatibility
	 * layer over the AnalysisCache.
	 * @param project 			The project
	 * @param appClassList       list of ClassDescriptors identifying application classes
	 * @param sourceInfoFileName name of source info file (null if none)
	 */
	public static void createAnalysisContext(
			Project project,
			List<ClassDescriptor> appClassList, String sourceInfoFileName)
			throws CheckedAnalysisException, IOException {
		AnalysisCacheToAnalysisContextAdapter analysisContext =
			new AnalysisCacheToAnalysisContextAdapter();

		// Make this the current analysis context
		AnalysisContext.setCurrentAnalysisContext(analysisContext);

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
		analysisContext.setSourcePath(project.getSourceDirList());
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
			HashSet<DetectorFactory> forcedEnabled = new HashSet<DetectorFactory>();
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.DetectorFactoryChooser#choose(edu.umd.cs.findbugs.DetectorFactory)
			 */
			public boolean choose(DetectorFactory factory) {
				return FindBugs.isDetectorEnabled(FindBugs2.this, factory) || forcedEnabled.contains(factory);
			}
			public void enable(DetectorFactory factory) {
				forcedEnabled.add(factory);
				factory.setEnabledButNonReporting(true);
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
		
		// Stash the ExecutionPlan in the AnalysisCache.
		Global.getAnalysisCache().eagerlyPutDatabase(ExecutionPlan.class, executionPlan);

		if (DEBUG) {
			System.out.println(executionPlan.getNumPasses() + " passes in execution plan");
		}
	}

	static private final boolean USE_REFERENCES = SystemProperties.getBoolean("tsort.references");

	/**
	 * Analyze the classes in the application codebase.
	 */
	private void analyzeApplication() throws InterruptedException {
		int passCount = 0;
		Profiler profiler = Profiler.getInstance();
		profiler.start(this.getClass());
		AnalysisContext.currentXFactory().canonicalizeAll();
		try {
			boolean multiplePasses = executionPlan.getNumPasses() > 1;
			if (executionPlan.getNumPasses() == 0) {
				throw new AssertionError("no analysis passes");
			}
			int[] classesPerPass = new int[executionPlan.getNumPasses()];
			classesPerPass[0] = referencedClassSet.size();
			for (int i = 0; i < classesPerPass.length; i++) {
				classesPerPass[i] = i == 0 ? referencedClassSet.size() : appClassList.size();
			}
			progress.predictPassCount(classesPerPass);
			XFactory factory = AnalysisContext.currentXFactory();
			for (ClassDescriptor desc : referencedClassSet) {

				try {
					XClass info = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc);
					factory.intern(info);
				} catch (CheckedAnalysisException e) {
					AnalysisContext.logError("Couldn't get class info for " + desc, e);
				}


			}
			for (Iterator<AnalysisPass> i = executionPlan.passIterator(); i.hasNext();) {
				AnalysisPass pass = i.next();

				// The first pass is generally a non-reporting pass which
				// gathers information about referenced classes.
				boolean isNonReportingFirstPass = multiplePasses && passCount == 0;

				// Instantiate the detectors
				Detector2[] detectorList = pass.instantiateDetector2sInPass(bugReporter);

				// If there are multiple passes, then on the first pass,
				// we apply detectors to all classes referenced by the application classes.
				// On subsequent passes, we apply detector only to application classes.
				Collection<ClassDescriptor> classCollection = (isNonReportingFirstPass)
					? referencedClassSet
					: appClassList;
				AnalysisContext.currentXFactory().canonicalizeAll();
				if (DEBUG || LIST_ORDER) {
					System.out.println("Pass " + (passCount) + ": " + classCollection.size() + " classes");
					XFactory.profile();
				}
				if (!isNonReportingFirstPass) {
					OutEdges<ClassDescriptor> outEdges = new OutEdges<ClassDescriptor>() {

						public Collection<ClassDescriptor> getOutEdges(ClassDescriptor e) {
							try {
								XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, e);
								if (classNameAndInfo instanceof ClassNameAndSuperclassInfo) {
									return ((ClassNameAndSuperclassInfo) classNameAndInfo).getCalledClassDescriptorList();
								}
								assert false;
								return Collections.<ClassDescriptor>emptyList();
							} catch (CheckedAnalysisException e2) {
								AnalysisContext.logError("error while analyzing " + e.getClassName(), e2);
								return Collections.<ClassDescriptor>emptyList();

							}
						}
					};
					List<ClassDescriptor> result = sortByCallGraph(classCollection, outEdges);

					classCollection = result;
				}
				if (LIST_ORDER) {
					System.out.println("Analysis order:");
					for (ClassDescriptor c : classCollection) {
						System.out.println("  " + c);
					}
				}
				progress.startAnalysis(classCollection.size());
				int count = 0;
				Global.getAnalysisCache().purgeAllMethodAnalysis();
				for (ClassDescriptor classDescriptor : classCollection) {
					if (DEBUG) {
						System.out.println(count + "/" + classCollection.size() + ": Class " + classDescriptor);
						count++;
					}

					// Check to see if class is excluded by the class screener.
					// In general, we do not want to screen classes from the
					// first pass, even if they would otherwise be excluded.
					if ((SCREEN_FIRST_PASS_CLASSES || !isNonReportingFirstPass) && !classScreener.matches(classDescriptor.toResourceName())) {
						if (DEBUG) {
							System.out.println("*** Excluded by class screener");
						}
						continue;
					}
					boolean isHuge = AnalysisContext.currentAnalysisContext().isTooBig(classDescriptor);
					if (isHuge && AnalysisContext.currentAnalysisContext().isApplicationClass(classDescriptor)) {
						bugReporter.reportBug(new BugInstance("SKIPPED_CLASS_TOO_BIG", Priorities.NORMAL_PRIORITY).addClass(classDescriptor));
					}
					currentClassName = ClassName.toDottedClassName(classDescriptor.getClassName());
					notifyClassObservers(classDescriptor);


					for (Detector2 detector : detectorList) {
						if (Thread.interrupted()) {
							throw new InterruptedException();
						}
						if (isHuge && !NonReportingDetector.class.isAssignableFrom(detector.getClass())) {

							continue;
						}
						if (DEBUG) {
							System.out.println("Applying " + detector.getDetectorClassName() + " to " + classDescriptor);
							//System.out.println("foo: " + NonReportingDetector.class.isAssignableFrom(detector.getClass()) + ", bar: " + detector.getClass().getName());
						}
						try {
							profiler.start(detector.getClass());
							detector.visitClass(classDescriptor);
						} catch (ClassFormatException e) {
							logRecoverableException(classDescriptor, detector, e);
						} catch (MissingClassException e) {
							Global.getAnalysisCache().getErrorLogger().reportMissingClass(e.getClassDescriptor());
						} catch (CheckedAnalysisException e) {
							logRecoverableException(classDescriptor, detector, e);
						} catch (RuntimeException e) {
							logRecoverableException(classDescriptor, detector, e);
						} finally {
							profiler.end(detector.getClass());
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
			
			// If the user requested it,
			// try to load user annotations from the loaded
			// user annotation plugin.
			if (userAnnotationPlugin != null && userAnnotationSync) {
				syncUserAnnotations();
			}

			// Flush any queued bug reports
			bugReporter.finish();

			// if (baselineBugs != null) new Update().removeBaselineBugs(baselineBugs, bugReporter.);
			// Flush any queued error reports
			bugReporter.reportQueuedErrors();
		} finally {
			profiler.end(this.getClass());
		}

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
		// Sanity-check the loaded BCEL classes
		if(!CheckBcel.check()) {
			System.exit(1);
		}

		// Create FindBugs2 engine
		FindBugs2 findBugs = new FindBugs2();

		// Parse command line and configure the engine
		TextUICommandLine commandLine = new TextUICommandLine();
		FindBugs.processCommandLine(commandLine, args, findBugs);

		// Away we go!
		FindBugs.runMain(findBugs, commandLine);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setAbridgedMessages(boolean)
	 */
	public void setAbridgedMessages(boolean xmlWithAbridgedMessages) {
		abridgedMessages = xmlWithAbridgedMessages;

	}

	private void syncUserAnnotations() {
		if (DEBUG_UA) {
			System.out.println("About to sync user annotations...");
		}
		
		BugReporter realBugReporter = bugReporter.getRealBugReporter();

		if (!(realBugReporter instanceof BugCollectionBugReporter)) {
			bugReporter.logError("Cannot load user annotations because there is no BugCollection: use -xml output option");
		} else {
			BugCollection bugCollection = ((BugCollectionBugReporter) realBugReporter).getBugCollection();

			try {
				userAnnotationPlugin.loadUserAnnotations(bugCollection);
			} catch (Exception e) {
				bugReporter.logError("Could not sync user annotations using plugin", e);
			}
		}
	}


}
