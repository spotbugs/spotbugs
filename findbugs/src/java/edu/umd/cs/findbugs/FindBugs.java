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

import java.io.*;
import java.util.*;
import java.util.zip.*;
import edu.umd.cs.pugh.io.IO;
import edu.umd.cs.pugh.visitclass.Constants2;
import org.apache.bcel.classfile.*;
import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

/**
 * An instance of this class is used to apply the selected set of
 * analyses on some collection of Java classes.  It also implements the
 * comand line interface.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class FindBugs implements Constants2, ExitCodes
{
  /* ----------------------------------------------------------------------
   * Helper classes
   * ---------------------------------------------------------------------- */

  /**
   * Interface for an object representing a source of class files to analyze.
   */
  private interface ClassProducer {
	/**
	 * Get the next class to analyze.
	 * @return the class, or null of there are no more classes for this ClassProducer
	 * @throws IOException if an IOException occurs
	 * @throws InterruptedException if the thread is interrupted
	 */
	public JavaClass getNextClass() throws IOException, InterruptedException;
  }

  /**
   * ClassProducer for single class files.
   */
  private static class SingleClassProducer implements ClassProducer {
	private String fileName;

	/**
	 * Constructor.
	 * @param fileName the single class file to be analyzed
	 */
	public SingleClassProducer(String fileName) {
		this.fileName = fileName;
	}

	public JavaClass getNextClass() throws IOException, InterruptedException {
		if (fileName == null)
			return null;
		if (Thread.interrupted())
			throw new InterruptedException();

		String fileNameToParse = fileName;
		fileName = null; // don't return it next time

		try {
			return new ClassParser(fileNameToParse).parse();
		} catch (ClassFormatException e) {
			throw new ClassFormatException("Invalid class file format for " +
				fileNameToParse + ": " + e.getMessage());
		}
	}
  }

  /**
   * ClassProducer for .zip and .jar files.
   */
  private static class ZipClassProducer implements ClassProducer {
	private String fileName;
	private ZipFile zipFile;
	private Enumeration entries;

	/**
	 * Constructor.
	 * @param fileName the name of the zip or jar file
	 */
	public ZipClassProducer(String fileName) throws IOException {
		this.fileName = fileName;
		this.zipFile = new ZipFile(fileName);
		this.entries = zipFile.entries();
	}

	public JavaClass getNextClass() throws IOException, InterruptedException {
		ZipEntry classEntry = null;
		while (classEntry == null && entries.hasMoreElements()) {
			if (Thread.interrupted())
				throw new InterruptedException();
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (entry.getName().endsWith(".class"))
				classEntry = entry;
		}
		if (classEntry == null)
			return null;
		String fullEntryName = fileName + ":" + classEntry.getName();
		try {
			return new ClassParser(zipFile.getInputStream(classEntry), classEntry.getName()).parse();
		} catch (ClassFormatException e) {
			throw new ClassFormatException("Invalid class file format for " +
				fullEntryName + ": " + e.getMessage());
		}
	}
  }

  /**
   * ClassProducer for directories.
   * The directory is scanned recursively for class files.
   */
  private static class DirectoryClassProducer implements ClassProducer {
	private Iterator<String> rfsIter;

	public DirectoryClassProducer(String dirName) throws InterruptedException {
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".class");
			}
		};

		// This will throw InterruptedException if the thread is
		// interrupted.
		RecursiveFileSearch rfs = new RecursiveFileSearch(dirName, filter).search();
		this.rfsIter = rfs.fileNameIterator();
	}

	public JavaClass getNextClass() throws IOException, InterruptedException {
		if (!rfsIter.hasNext())
			return null;
		String fileName = rfsIter.next();
		try {
			return new ClassParser(fileName).parse();
		} catch (ClassFormatException e) {
			throw new ClassFormatException("Invalid class file format for " +
				fileName + ": " + e.getMessage());
		}
	}
  }

  /**
   * A delegating bug reporter which counts reported bug instances,
   * missing classes, and serious analysis errors.
   */
  private static class ErrorCountingBugReporter extends DelegatingBugReporter {
	private int bugCount;
	private int missingClassCount;
	private int errorCount;

	public ErrorCountingBugReporter(BugReporter realBugReporter) {
		super(realBugReporter);
		this.bugCount = 0;
		this.missingClassCount = 0;
		this.errorCount = 0;

		// Add an observer to record when bugs make it through
		// all priority and filter criteria, so our bug count is
		// accurate.
		realBugReporter.addObserver(new BugReporterObserver() {
			public void reportBug(BugInstance bugInstance) {
				++bugCount;
			}
		});
	}

	public int getBugCount() {
		return bugCount;
	}

	public int getMissingClassCount() {
		return missingClassCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public void logError(String message) {
		++errorCount;
		super.logError(message);
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		++missingClassCount;
		super.reportMissingClass(ex);
	}
  }

  /* ----------------------------------------------------------------------
   * Member variables
   * ---------------------------------------------------------------------- */

  private static final boolean DEBUG = Boolean.getBoolean("findbugs.debug");

  /** FindBugs home directory. */
  private static String home;

  private ErrorCountingBugReporter bugReporter;
  private Project project;
  private Detector detectors [];
  private FindBugsProgress progressCallback;

  /* ----------------------------------------------------------------------
   * Public methods
   * ---------------------------------------------------------------------- */

  /**
   * Constructor.
   * @param bugReporter the BugReporter object that will be used to report
   *   BugInstance objects, analysis errors, class to source mapping, etc.
   * @param project the Project indicating which files to analyze and
   *   the auxiliary classpath to use
   */
  public FindBugs(BugReporter bugReporter, Project project) {
	if (bugReporter == null)
		throw new IllegalArgumentException("null bugReporter");
	if (project == null)
		throw new IllegalArgumentException("null project");

	this.bugReporter = new ErrorCountingBugReporter(bugReporter);
	this.project = project;

	// Create a no-op progress callback.
	this.progressCallback = new FindBugsProgress() {
		public void reportNumberOfArchives(int numArchives) { }
		public void finishArchive() { }
		public void startAnalysis(int numClasses) { }
		public void finishClass() { }
		public void finishPerClassAnalysis() { }
	};
  }

  /**
   * Set the progress callback that will be used to keep track
   * of the progress of the analysis.
   * @param progressCallback the progress callback
   */
  public void setProgressCallback(FindBugsProgress progressCallback) {
	this.progressCallback = progressCallback;
  }

  /**
   * Set filter of bug instances to include or exclude.
   * @param filterFileName the name of the filter file
   * @param include true if the filter specifies bug instances to include,
   *   false if it specifies bug instances to exclude
   */
  public void setFilter(String filterFileName, boolean include) throws IOException, FilterException {
	Filter filter = new Filter(filterFileName);
	BugReporter origBugReporter = bugReporter.getRealBugReporter();
	BugReporter filterBugReporter = new FilterBugReporter(origBugReporter, filter, include);
	bugReporter.setRealBugReporter(filterBugReporter);
  }

  /**
   * Execute FindBugs on the Project.
   * All bugs found are reported to the BugReporter object which was set
   * when this object was constructed.
   * @throws java.io.IOException if an I/O exception occurs analyzing one of the files
   * @throws InterruptedException if the thread is interrupted while conducting the analysis
   */
  public void execute() throws java.io.IOException, InterruptedException {
	if (detectors == null)
		createDetectors();

	clearRepository();

	String[] argv = project.getJarFileArray();

	progressCallback.reportNumberOfArchives(argv.length);

	List<String> repositoryClassList = new LinkedList<String>();

	for (int i = 0; i < argv.length; i++) {
		addFileToRepository(argv[i], repositoryClassList);
		}

	progressCallback.startAnalysis(repositoryClassList.size());

	for (Iterator<String> i = repositoryClassList.iterator(); i.hasNext(); ) {
		String className = i.next();
		examineClass(className);
		}

	progressCallback.finishPerClassAnalysis();

	this.reportFinal();

	// Flush any queued bug reports
	bugReporter.finish();

	// Flush any queued error reports
	bugReporter.reportQueuedErrors();
  }

  /**
   * Get the number of bug instances that were reported during analysis.
   */
  public int getBugCount() {
	return bugReporter.getBugCount();
  }

  /**
   * Get the number of errors that occurred during analysis.
   */
  public int getErrorCount() {
	return bugReporter.getErrorCount();
  }

  /**
   * Get the number of time missing classes were reported during analysis.
   */
  public int getMissingClassCount() {
	return bugReporter.getMissingClassCount();
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
	if (home == null) {
		home = System.getProperty("findbugs.home");
		if (home == null) {
			System.err.println("Error: The findbugs.home property is not set!");
			System.exit(1);
		}
	}
	return home;
  }

  /* ----------------------------------------------------------------------
   * Private methods
   * ---------------------------------------------------------------------- */

  /**
   * Create Detectors for each DetectorFactory which is enabled.
   * This will populate the detectors array.
   */
  private void createDetectors() {
	ArrayList<Detector> result = new ArrayList<Detector>();

	Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
	int count = 0;
	while (i.hasNext()) {
		DetectorFactory factory = i.next();
		if (factory.isEnabled())
			result.add(factory.create(bugReporter));
	}

	detectors = result.toArray(new Detector[0]);
  }

  /**
   * Clear the Repository and update it to reflect the classpath
   * specified by the current project.
   */
  private void clearRepository() {
	// Purge repository of previous contents
	Repository.clearCache();

	// Create a SyntheticRepository based on the current project,
	// and make it current.

	// Add aux class path entries specified in project
	StringBuffer buf = new StringBuffer();
	List auxClasspathEntryList = project.getAuxClasspathEntryList();
	Iterator i = auxClasspathEntryList.iterator();
	while (i.hasNext()) {
	    String entry = (String) i.next();
	    buf.append(entry);
	    buf.append(File.pathSeparatorChar);
	}

	// Add the system classpath entries
	buf.append(ClassPath.getClassPath());

	// Set up the Repository to use the combined classpath
	ClassPath classPath = new ClassPath(buf.toString());
	SyntheticRepository repository = SyntheticRepository.getInstance(classPath);
	Repository.setRepository(repository);
  }

  /**
   * Add all classes contained in given file to the BCEL Repository.
   * @param fileName the file, which may be a jar/zip archive, a single class file,
   *   or a directory to be recursively searched for class files
   */
  private void addFileToRepository(String fileName, List<String> repositoryClassList)
	throws IOException, InterruptedException {

     try {
	ClassProducer classProducer;

	// Create the ClassProducer
	if (fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".war") || fileName.endsWith(".ear"))
		classProducer = new ZipClassProducer(fileName);
	else if (fileName.endsWith(".class"))
		classProducer = new SingleClassProducer(fileName);
	else {
		File dir = new File(fileName);
		if (!dir.isDirectory())
			throw new IOException("Path " + fileName + " is not an archive, class file, or directory");
		classProducer = new DirectoryClassProducer(fileName);
	}

	// Load all referenced classes into the Repository
	for (;;) {
		if (Thread.interrupted())
			throw new InterruptedException();
		try {
			JavaClass jclass = classProducer.getNextClass();
			if (jclass == null)
				break;
			Repository.addClass(jclass);
			repositoryClassList.add(jclass.getClassName());
		} catch (ClassFormatException e) {
			e.printStackTrace();
			bugReporter.logError(e.getMessage());
		}
	}

	progressCallback.finishArchive();

     } catch (IOException e) {
	// You'd think that the message for a FileNotFoundException would include
	// the filename, but you'd be wrong.  So, we'll add it explicitly.
	throw new IOException("Could not analyze " + fileName + ": " + e.getMessage());
     }
  }

  /**
   * Examine a single class by invoking all of the Detectors on it.
   * @param className the fully qualified name of the class to examine
   */
  private void examineClass(String className) throws InterruptedException {
	if (DEBUG) System.out.println("Examining class " + className);

	JavaClass javaClass;
	try {
		javaClass = Repository.lookupClass(className);
	} catch (ClassNotFoundException e) {
		throw new AnalysisException("Could not find class " + className + " in Repository", e);
	}

	ClassContext classContext = new ClassContext(javaClass);

	for (int i = 0; i < detectors.length; ++i) {
		if (Thread.interrupted())
			throw new InterruptedException();
		try {
			Detector detector = detectors[i];
			if (DEBUG) System.out.println("  running " + detector.getClass().getName());
			try {
				detector.visitClassContext(classContext);
			} catch (AnalysisException e) {
				bugReporter.logError(e.toString());
			}
		} catch (AnalysisException e) {
			bugReporter.logError("Analysis exception: " + e.toString());
		}
	}

	progressCallback.finishClass();
  }

  /**
   * Call report() on all detectors, to give them a chance to
   * report any accumulated bug reports.
   */
  private void reportFinal() throws InterruptedException {
	for (int i = 0; i < detectors.length; ++i) {
		if (Thread.interrupted())
			throw new InterruptedException();
		detectors[i].report();
	}
  }

  /* ----------------------------------------------------------------------
   * main() method
   * ---------------------------------------------------------------------- */

  private static final int PRINTING_REPORTER = 0;
  private static final int SORTING_REPORTER = 1;
  private static final int XML_REPORTER = 2;

  public static void main(String argv[]) throws Exception
  { 
	int bugReporterType = PRINTING_REPORTER;
	Project project = new Project();
	boolean quiet = false;
	String filterFile = null;
	boolean include = false;
	boolean setExitCode = false;
	int priorityThreshold = Detector.NORMAL_PRIORITY;

	// Process command line options
	int argCount = 0;
	while (argCount < argv.length) {
		String option = argv[argCount];
		if (!option.startsWith("-"))
			break;
		if (option.equals("-home")) {
			++argCount;
			if (argCount == argv.length) throw new IllegalArgumentException(option + " option requires argument");
			String homeDir = argv[argCount];
			FindBugs.setHome(homeDir);
		} else if (option.equals("-low"))
			priorityThreshold = Detector.LOW_PRIORITY;
		else if (option.equals("-medium"))
			priorityThreshold = Detector.NORMAL_PRIORITY;
		else if (option.equals("-high"))
			priorityThreshold = Detector.HIGH_PRIORITY;
		else if (option.equals("-sortByClass"))
			bugReporterType = SORTING_REPORTER;
		else if (option.equals("-xml"))
			bugReporterType = XML_REPORTER;
		else if (option.equals("-visitors") || option.equals("-omitVisitors")) {
			++argCount;
			if (argCount == argv.length) throw new IllegalArgumentException(option + " option requires argument");
			boolean omit = option.equals("-omitVisitors");

			if (!omit) {
				// Selecting detectors explicitly, so start out by
				// disabling all of them.  The selected ones will
				// be re-enabled.
				Iterator<DetectorFactory> factoryIter =
					DetectorFactoryCollection.instance().factoryIterator();
				while (factoryIter.hasNext()) {
					DetectorFactory factory = factoryIter.next();
					factory.setEnabled(false);
				}
			}

			// Explicitly enable or disable the selector detectors.
			StringTokenizer tok = new StringTokenizer(argv[argCount], ",");
			while (tok.hasMoreTokens()) {
				String visitorName = tok.nextToken();
				DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(visitorName);
				if (factory == null)
					throw new IllegalArgumentException("Unknown detector: " + visitorName);
				factory.setEnabled(!omit);
			}
		} else if (option.equals("-exclude") || option.equals("-include")) {
			++argCount;
			if (argCount == argv.length) throw new IllegalArgumentException(option + " option requires argument");
			filterFile = argv[argCount];
			include = option.equals("-include");
		} else if (option.equals("-quiet")) {
			quiet = true;
		} else if (option.equals("-auxclasspath")) {
			++argCount;
			if (argCount == argv.length)
				throw new IllegalArgumentException(option + " option requires argument");

			String auxClassPath = argv[argCount];
			StringTokenizer tok = new StringTokenizer(auxClassPath, File.pathSeparator);
			while (tok.hasMoreTokens())
				project.addAuxClasspathEntry(tok.nextToken());
		} else if (option.equals("-project")) {
			++argCount;
			if (argCount == argv.length)
				throw new IllegalArgumentException(option + " option requires argument");

			String projectFile = argv[argCount];

			// Convert project file to be an absolute path
			projectFile = new File(projectFile).getAbsolutePath();

			project = new Project(projectFile);
			project.read(new BufferedInputStream(new FileInputStream(projectFile)));
		} else if (option.equals("-exitcode")) {
			setExitCode = true;
		} else
			throw new IllegalArgumentException("Unknown option: " + option);
		++argCount;
	}

	if (argCount == argv.length && project.getNumJarFiles() == 0) {
		InputStream in = FindBugs.class.getClassLoader().getResourceAsStream("USAGE");
		if (in == null)  {
			System.out.println("FindBugs tool, version " + Version.RELEASE);
			System.out.println("usage: java -jar findbugs.jar [options] <classfiles, zip/jar files, or directories>");
			System.out.println("Example: java -jar findbugs.jar rt.jar");
			System.out.println("Options:");
			System.out.println("   -home <home directory>        specify FindBugs home directory");
			System.out.println("   -quiet                        suppress error messages");
			System.out.println("   -low                          report all bugs");
			System.out.println("   -medium                       report medium and high priority bugs [default]");
			System.out.println("   -high                         report high priority bugs only");
			System.out.println("   -sortByClass                  sort bug reports by class");
			System.out.println("   -xml                          XML output");
			System.out.println("   -visitors <v1>,<v2>,...       run only named visitors");
			System.out.println("   -omitVisitors <v1>,<v2>,...   omit named visitors");
			System.out.println("   -exclude <filter file>        exclude bugs matching given filter");
			System.out.println("   -include <filter file>        include only bugs matching given filter");
			System.out.println("   -auxclasspath <classpath>     set aux classpath for analysis");
			System.out.println("   -project <project>            analyze given project");
			System.out.println("   -exitcode                     set exit code of process");
			}
		else
			IO.copy(in,System.out);
		return;
		}

	BugReporter bugReporter = null;
	switch (bugReporterType) {
	case PRINTING_REPORTER:
		bugReporter = new PrintingBugReporter(); break;
	case SORTING_REPORTER:
		bugReporter = new SortingBugReporter(); break;
	case XML_REPORTER:
		bugReporter = new XMLBugReporter(project); break;
	default:
		throw new IllegalStateException();
	}

	if (quiet)
		bugReporter.setErrorVerbosity(BugReporter.SILENT);

	bugReporter.setPriorityThreshold(priorityThreshold);

	for (int i = argCount; i < argv.length; ++i)
		project.addJar(argv[i]);

	FindBugs findBugs = new FindBugs(bugReporter, project);

	if (filterFile != null)
		findBugs.setFilter(filterFile, include);

	findBugs.execute();

	int bugCount = findBugs.getBugCount();
	int missingClassCount = findBugs.getMissingClassCount();
	int errorCount = findBugs.getErrorCount();

	if (!quiet || setExitCode) {
		if (bugCount > 0)
			System.err.println("Bugs found: " + bugCount);
		if (missingClassCount > 0)
			System.err.println("Missing classes: " + missingClassCount);
		if (errorCount > 0)
			System.err.println("Analysis errors: " + errorCount);
	}

	if (setExitCode) {
		int exitCode = 0;
		if (errorCount > 0)
			exitCode |= ERROR_FLAG;
		if (missingClassCount > 0)
			exitCode |= MISSING_CLASS_FLAG;
		if (bugCount > 0)
			exitCode |= BUGS_FOUND_FLAG;

		System.exit(exitCode);
	}
  }
}
