package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class InitializationChain extends BytecodeScanningDetector implements   Constants2 {
    Set<String> requires = new TreeSet<String>();
    Map<String, Set<String>> classRequires = new TreeMap<String, Set<String>>();
    private BugReporter bugReporter;

    private static final boolean DEBUG = Boolean.getBoolean("ic.debug");

    public InitializationChain(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
    }

    public void visit(Code obj) {
        if (!methodName.equals("<clinit>")) return;
        super.visit(obj);
	requires.remove(betterClassName);
	if (betterClassName.equals("java.lang.System")) {
		requires.add("java.io.FileInputStream");
		requires.add("java.io.FileOutputStream");
		requires.add("java.io.BufferedInputStream");
		requires.add("java.io.BufferedOutputStream");
		requires.add("java.io.PrintStream");
		}
	if (!requires.isEmpty()) {
	  classRequires.put(betterClassName,requires);
	  requires = new TreeSet();
	  }
    }


    public void sawOpcode(int seen) {
	if (PC + 6 >= codeBytes.length) return;

        if (seen == PUTSTATIC || seen == GETSTATIC || seen == INVOKESTATIC
			|| seen == NEW)  
		requires.add(betterClassConstant);
	}

    public void compute() {
	Set<String> allClasses = classRequires.keySet();
	Set<String> emptyClasses = new TreeSet<String>();
	for(Iterator i = allClasses.iterator(); i.hasNext(); ) {
		String c = (String) i.next();
		Set needs = (Set) classRequires.get(c);
		needs.retainAll(allClasses);
		Set extra = new TreeSet();
		for(Iterator j = needs.iterator(); j.hasNext(); ) 
			extra.addAll((Set)classRequires.get(j.next()));
		needs.addAll(extra);
		needs.retainAll(allClasses);
		classRequires.put(c,needs);
		if (needs.isEmpty()) emptyClasses.add(c);
		}
	for(Iterator i = emptyClasses.iterator(); i.hasNext(); ) {
		String c = (String) i.next();
		classRequires.remove(c);
		}
	}

    public void report() {

	if (DEBUG) System.out.println("Finishing computation");
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();
	Set allClasses = classRequires.keySet();

	for(Iterator<String> i = allClasses.iterator(); i.hasNext(); ) {
		String c = i.next();
        	if (DEBUG) System.out.println("Class " + c + " requires:");
		for(Iterator<String> j = (classRequires.get(c)).iterator(); 
				j.hasNext(); ) {
		    String needs = j.next();
                    if (DEBUG) System.out.println("  " + needs);
		    Set<String> s = classRequires.get(needs);
		    if (s != null && s.contains(c) && c.compareTo(needs) < 0)
			bugReporter.reportBug(new BugInstance("IC_INIT_CIRCULARITY", NORMAL_PRIORITY)
				.addClass(c)
				.addClass(needs));
		    }
		}
	}

    }
