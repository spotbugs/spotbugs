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
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.zip.*;
import edu.umd.cs.pugh.io.IO;
import edu.umd.cs.pugh.visitclass.Constants2;
import org.apache.bcel.classfile.*;
import org.apache.bcel.Repository;

public class FindBugs implements Constants2
{
  private final BugReporter bugReporter;
  private Detector detectors [];
  private LinkedList<String> detectorNames;
  private boolean omit;
  private HashMap<String, String> classNameToSourceFileMap;
  private FindBugsProgress progressCallback;

  public FindBugs(BugReporter bugReporter, LinkedList<String> detectorNames, boolean omit) {
	if (bugReporter == null)
		throw new IllegalArgumentException("null bugReporter");
	this.bugReporter = bugReporter;
	this.detectorNames = detectorNames;
	this.omit = omit;
	this.classNameToSourceFileMap = new HashMap<String, String>();

	// Create a no-op progress callback.
	this.progressCallback = new FindBugsProgress() {
		public void reportNumberOfArchives(int numArchives) { }
		public void finishArchive() { }
		public void startAnalysis(int numClasses) { }
		public void finishClass() { }
		public void finishPerClassAnalysis() { }
	};
  }

  public FindBugs(BugReporter bugReporter) {
	this(bugReporter, null, false);
  }

  /**
   * Set the progress callback that will be used to keep track
   * of the progress of the analysis.
   * @param progressCallback the progress callback
   */
  public void setProgressCallback(FindBugsProgress progressCallback) {
	this.progressCallback = progressCallback;
  }

  private static ArrayList<Class> factories = new ArrayList<Class>();
  private static HashMap<String, Class> factoriesByName = new HashMap<String, Class>();
  private static IdentityHashMap<Class, String> namesByFactory = new IdentityHashMap<Class, String>();

  private static Class [] constructorArgTypes = {BugReporter.class};

  private Detector makeDetector(Class c) {
	try {
	if (!Detector.class.isAssignableFrom(c))
		throw new IllegalStateException(c + " is not a Detector");
	Constructor constructor = c.getConstructor(constructorArgTypes);
	return (Detector) constructor.newInstance(
				new Object[] {bugReporter});
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
		}
	}
  public static void registerDetector(String detectorName, Class detector)  {
	factories.add(detector);
	factoriesByName.put(detectorName, detector);
	namesByFactory.put(detector, detectorName);
  }

  static {
    registerDetector("FindFinalizeInvocations", edu.umd.cs.findbugs.FindFinalizeInvocations.class);
    registerDetector("MutableLock", edu.umd.cs.findbugs.MutableLock.class);
    registerDetector("FindUnsyncGet", edu.umd.cs.findbugs.FindUnsyncGet.class);
    registerDetector("InitializationChain", edu.umd.cs.findbugs.InitializationChain.class);
    //registerDetector("LockedFields", edu.umd.cs.findbugs.LockedFields.class);
    registerDetector("FindHEmismatch", edu.umd.cs.findbugs.FindHEmismatch.class);
    registerDetector("DumbMethods", edu.umd.cs.findbugs.DumbMethods.class);
    registerDetector("FindUninitializedGet", edu.umd.cs.findbugs.FindUninitializedGet.class);
    registerDetector("ReadReturnShouldBeChecked", edu.umd.cs.findbugs.ReadReturnShouldBeChecked.class);
    registerDetector("FindNakedNotify", edu.umd.cs.findbugs.FindNakedNotify.class);
    registerDetector("FindUnconditionalWait", edu.umd.cs.findbugs.FindUnconditionalWait.class);
    registerDetector("FindSpinLoop", edu.umd.cs.findbugs.FindSpinLoop.class);
    registerDetector("FindDoubleCheck", edu.umd.cs.findbugs.FindDoubleCheck.class);
    registerDetector("WaitInLoop", edu.umd.cs.findbugs.WaitInLoop.class);
    registerDetector("DroppedException", edu.umd.cs.findbugs.DroppedException.class);
    registerDetector("FindRunInvocations", edu.umd.cs.findbugs.FindRunInvocations.class);
    registerDetector("IteratorIdioms", edu.umd.cs.findbugs.IteratorIdioms.class);
    registerDetector("SerializableIdiom", edu.umd.cs.findbugs.SerializableIdiom.class);
    registerDetector("StartInConstructor", edu.umd.cs.findbugs.StartInConstructor.class);
    registerDetector("FindReturnRef", edu.umd.cs.findbugs.FindReturnRef.class);
    registerDetector("Naming", edu.umd.cs.findbugs.Naming.class);
    registerDetector("UnreadFields", edu.umd.cs.findbugs.UnreadFields.class);
    registerDetector("MutableStaticFields", edu.umd.cs.findbugs.MutableStaticFields.class);
    registerDetector("SimplePathsFindDoubleCheck", edu.umd.cs.findbugs.SimplePathsFindDoubleCheck.class);
    registerDetector("FindTwoLockWait", edu.umd.cs.findbugs.FindTwoLockWait.class);
    registerDetector("FindInconsistentSync", edu.umd.cs.findbugs.FindInconsistentSync.class);
  }

  private void createDetectors() {
    if (detectorNames == null) {
	// Detectors were not named explicitly on command line,
	// so create all of them.
	detectors = new Detector[factories.size()];
	Iterator i = factories.iterator();
	int count = 0;
	while (i.hasNext())
		detectors[count++] = makeDetector((Class) i.next());
    } else {
	// Detectors were named explicitly on command line.

	if (!omit) {
		// Create only named detectors.
		detectors = new Detector[detectorNames.size()];
		Iterator i = detectorNames.iterator();
		int count = 0;
		while (i.hasNext()) {
			String name = (String) i.next();
			Class factory = (Class) factoriesByName.get(name);
			if (factory == null)
				throw new IllegalArgumentException("No such detector: " + name);
			detectors[count++] = makeDetector(factory);
		}
	} else {
		// Create all detectors EXCEPT named detectors.
		int numDetectors = factories.size() - detectorNames.size();
		detectors = new Detector[numDetectors];
		Iterator i = factories.iterator();
		int count = 0;
		while (i.hasNext()) {
			Class factory = (Class) i.next();
			String name = (String) namesByFactory.get(factory);
			if (!detectorNames.contains(name)) {
				// Add the detector.
				if (count == detectors.length)
					throw new IllegalArgumentException("bad omit list - nonexistent or duplicate detector specified?");
				detectors[count++] = makeDetector(factory);
			}
		}
		if (count != numDetectors) throw new IllegalStateException();
	}
    }
  }

  /**
   * Get the source file in which the given class is defined.
   * @param className fully qualified class name
   * @return name of the source file in which the class is defined
   */
  public String getSourceFile(String className) {
	return classNameToSourceFileMap.get(className);
  }

  /**
   * Add all classes contained in given file to the BCEL Repository.
   * @param fileName the file, which may be a jar/zip archive or a single class file
   */
  private void addFileToRepository(String fileName, List<String> repositoryClassList)
	throws IOException, InterruptedException {

	if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
		ZipFile zipFile = new ZipFile(fileName);
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			if (Thread.interrupted())
				throw new InterruptedException();

			ZipEntry entry = (ZipEntry) entries.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith(".class")) {
				InputStream in = zipFile.getInputStream(entry);
				JavaClass javaClass = new ClassParser(in, entryName).parse();
				Repository.addClass(javaClass);
				repositoryClassList.add(javaClass.getClassName());
			}
		}
	} else {
		if (Thread.interrupted())
			throw new InterruptedException();
		JavaClass javaClass = new ClassParser(fileName).parse();
		Repository.addClass(javaClass);
		repositoryClassList.add(javaClass.getClassName());
	}

	progressCallback.finishArchive();
  }

  /**
   * Examine a single class by invoking all of the Detectors on it.
   * @param className the fully qualified name of the class to examine
   */
  private void examineClass(String className) throws InterruptedException {
	JavaClass javaClass = Repository.lookupClass(className);
	if (javaClass == null)
		throw new AnalysisException("Could not find class " + className + " in Repository");

	classNameToSourceFileMap.put(javaClass.getClassName(), javaClass.getSourceFileName());
	ClassContext classContext = new ClassContext(javaClass);

	for (int i = 0; i < detectors.length; ++i) {
		if (Thread.interrupted())
			throw new InterruptedException();
		try {
			detectors[i].visitClassContext(classContext);
		} catch (AnalysisException e) {
			bugReporter.logError("Analysis exception: " + e.getMessage());
		}
	}

	progressCallback.finishClass();
  }

  /**
   * Call report() on all detectors, to give them a chance to
   * report any accumulated bug reports.
   */
  public void reportFinal() throws InterruptedException {
	for (int i = 0; i < detectors.length; ++i) {
		if (Thread.interrupted())
			throw new InterruptedException();
		detectors[i].report();
	}
  }

  /**
   * Execute FindBugs on given list of files (which may be jar files or class files).
   * All bugs found are reported to the BugReporter object which was set
   * when this object was constructed.
   * @param argv list of files to analyze
   * @throws java.io.IOException if an I/O exception occurs analyzing one of the files
   * @throws InterruptedException if the thread is interrupted while conducting the analysis
   */
  public void execute(String[] argv) throws java.io.IOException, InterruptedException {
	if (detectors == null)
		createDetectors();

	// Purge repository of previous contents
	Repository.clearCache();

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
  }

  public static void main(String argv[]) throws Exception
  { 
	boolean sortByClass = false;
	LinkedList<String> visitorNames = null;
	boolean omit = false;

	// Process command line options
	int argCount = 0;
	while (argCount < argv.length) {
		String option = argv[argCount];
		if (!option.startsWith("-"))
			break;
		if (option.equals("-sortByClass"))
			sortByClass = true;
		else if (option.equals("-visitors") || option.equals("-omitVisitors")) {
			++argCount;
			if (argCount == argv.length) throw new IllegalArgumentException(option + " option requires argument");
			omit = option.equals("-omitVisitors");
			StringTokenizer tok = new StringTokenizer(argv[argCount], ",");
			visitorNames = new LinkedList<String>();
			while (tok.hasMoreTokens())
				visitorNames.add(tok.nextToken());
		} else
			throw new IllegalArgumentException("Unknown option: " + option);
		++argCount;
	}

	if (argCount == argv.length) {
		InputStream in = FindBugs.class.getClassLoader().getResourceAsStream("USAGE");
		if (in == null)  {
			System.out.println("FindBugs tool, version 0.4");
			System.out.println("usage: java -jar FindBugs.jar [options] <classfiles, zip files or jar files>");
			System.out.println("Example: java -jar FindBugs.jar rt.jar");
			System.out.println("Options:");
			System.out.println("   -sortByClass                           sort bug reports by class");
			System.out.println("   -visitors <visitor 1>,<visitor 2>,...  run only named visitors");
			System.out.println("   -omitVisitors <v1>,<v2>,...            omit named visitors");
			}
		else
			IO.copy(in,System.out);
		return;
		}

	BugReporter bugReporter = sortByClass ? (BugReporter)new SortingBugReporter() : (BugReporter)new PrintingBugReporter();

	FindBugs findBugs = new FindBugs(bugReporter, visitorNames, omit);

	String[] fileList = new String[argv.length - argCount];
	System.arraycopy(argv, argCount, fileList, 0, fileList.length);

	findBugs.execute(fileList);

  }
}
