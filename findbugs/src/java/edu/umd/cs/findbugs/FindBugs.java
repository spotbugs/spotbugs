package edu.umd.cs.findbugs;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.zip.*;
import edu.umd.cs.pugh.io.IO;
import edu.umd.cs.pugh.visitclass.Constants2;
import org.apache.bcel.classfile.*;

public class FindBugs implements Constants2
{
  private final BugReporter bugReporter;
  private Detector detectors [];
  private LinkedList<String> detectorNames;
  private boolean omit;
  private HashMap<String, String> classNameToSourceFileMap;

  public FindBugs(BugReporter bugReporter, LinkedList<String> detectorNames, boolean omit) {
	if (bugReporter == null)
		throw new IllegalArgumentException("null bugReporter");
	this.bugReporter = bugReporter;
	this.detectorNames = detectorNames;
	this.omit = omit;
	this.classNameToSourceFileMap = new HashMap<String, String>();
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
    registerDetector("LockedFields", edu.umd.cs.findbugs.LockedFields.class);
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
/*
    registerDetector("FindInconsistentSync", 
      edu.umd.cs.daveho.findbugs.FindInconsistentSync.class);
*/
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

  public void examine(JavaClass c) {
	if (detectors == null)
		createDetectors();

	classNameToSourceFileMap.put(c.getClassName(), c.getSourceFileName());

	ClassContext classContext = new ClassContext(c);

	for(int i = 0; i < detectors.length; i++)  {
		Detector detector = detectors[i];
		detector.visitClassContext(classContext);
		}
	}

  public void examineFile(String fileName) throws IOException {
	if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
		//if (argv.length > 1) System.out.println(fileName);
		ZipFile z = new ZipFile(fileName);
		TreeSet<ZipEntry> zipEntries = new TreeSet<ZipEntry>(new Comparator<ZipEntry>() {
			public int compare(ZipEntry e1, ZipEntry e2) {
					String s1 = e1.getName();
					int pos1 = s1.lastIndexOf('/');
					String p1 = "null";
					if(pos1 >= 0)
						p1 = s1.substring(0,pos1);

					String s2 = e2.getName();
					int pos2 = s2.lastIndexOf('/');
					String p2 = "null";
					if(pos2 >= 0)
						p2 = s2.substring(0,pos2);
					int r = p1.compareTo(p2);
					if (r != 0) return r;
					return s1.compareTo(s2);
					}
			});
		for( Enumeration<ZipEntry> e = z.entries(); e.hasMoreElements(); ) 
			zipEntries.add(e.nextElement());
			
		for( Iterator j = zipEntries.iterator(); j.hasNext(); ) {
			ZipEntry ze = (ZipEntry)j.next();
			String name = ze.getName();
			if (name.endsWith(".class")) {
				examine(
				new ClassParser(z.getInputStream(ze),name).parse());
				}
		}
		}
	else
		examine( new ClassParser(fileName).parse());
  }

  public void reportFinal() {
	for (int i = 0; i < detectors.length; ++i) {
		detectors[i].report();
	}
  }

  public String getSourceFile(String className) {
	return classNameToSourceFileMap.get(className);
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

	for(int i=argCount; i < argv.length; i++) {
		findBugs.examineFile(argv[i]);
		}  

	findBugs.reportFinal();

	// Flush any queued bug reports
	bugReporter.finish();

  }
}
