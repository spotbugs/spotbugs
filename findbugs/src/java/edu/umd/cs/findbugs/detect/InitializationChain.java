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

package edu.umd.cs.findbugs.detect;
import edu.umd.cs.findbugs.*;
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
    private boolean instanceCreated;
    private boolean instanceCreatedWarningGiven;

    private static final boolean DEBUG = Boolean.getBoolean("ic.debug");

    public InitializationChain(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
    }

    public void visit(Code obj) {
	instanceCreated = false;
	instanceCreatedWarningGiven = false;
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
	  requires = new TreeSet<String>();
	  }
    }


    public void sawOpcode(int seen) {


	if (seen == PUTSTATIC)
		System.out.println("Saw putstatic " + nameConstant);
        if (seen == PUTSTATIC && classConstant.equals(className))  {
		if (instanceCreated && !instanceCreatedWarningGiven)  {
			String okSig = "L" + className + ";";
			if (!okSig.equals(sigConstant)) {
			  System.out.println("Instance created in static initializer before static field " + nameConstant + " assigned");
			  System.out.println("Class is " + className);
			  instanceCreatedWarningGiven = true;
			  }
			}
		}
        else if (seen == NEW && classConstant.equals(className))  {
		instanceCreated = true;
		}
        else if (seen == PUTSTATIC || seen == GETSTATIC || seen == INVOKESTATIC
			|| seen == NEW)  
		if (PC + 6 < codeBytes.length) 
			requires.add(betterClassConstant);
	}

    public void compute() {
	Set<String> allClasses = classRequires.keySet();
	Set<String> emptyClasses = new TreeSet<String>();
	for(Iterator i = allClasses.iterator(); i.hasNext(); ) {
		String c = (String) i.next();
		Set<String> needs = classRequires.get(c);
		needs.retainAll(allClasses);
		Set<String> extra = new TreeSet<String>();
		for(Iterator j = needs.iterator(); j.hasNext(); ) 
			extra.addAll(classRequires.get(j.next()));
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
	Set<String> allClasses = classRequires.keySet();

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
