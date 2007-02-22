/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.config.CommandLine;


/**
 * A simple BugReporter which simply prints the formatted message
 * to the output stream.
 */
public class PrintingBugReporter extends TextUIBugReporter {
	private String stylesheet = null;
	private boolean annotationUploadFormat = false;
  	private HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

	public void observeClass(ClassDescriptor classDescriptor) {
		// Don't need to do anything special, since we won't be
		// reporting statistics.
	}

	@Override
	protected void doReportBug(BugInstance bugInstance) {
		if (seenAlready.add(bugInstance)) {
			printBug(bugInstance);
			notifyObservers(bugInstance);
		}
	}

	public void finish() {
		outputStream.close();
	}
	
	class PrintingCommandLine extends CommandLine {

		public PrintingCommandLine() {
			addSwitch("-longBugCodes", "use long bug codes when generating text");
            addSwitch("-designations", "report user designations for each bug");
            addSwitch("-history", "report first and last versions for each bug");
            
			addSwitch("-annotationUpload", "generate annotations in upload format");
			addSwitchWithOptionalExtraPart("-html", "stylesheet",
			"Generate HTML output (default stylesheet is default.xsl)");
		}
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-longBugCodes"))
				setUseLongBugCodes(true);
			else if (option.equals("-designations"))
				setReportUserDesignations(true);
            else if (option.equals("-history"))
                setReportHistory(true);
           else if (option.equals("-annotationUpload"))
                annotationUploadFormat = true;
            else if (option.equals("-html")) {
				if (!optionExtraPart.equals("")) {
					stylesheet = optionExtraPart;
				} else {
					stylesheet = "default.xsl";
				}
			} else throw new IllegalArgumentException("Unknown option '"+option+"'");
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			// TODO Auto-generated method stub
			
		}
		
	}
	public static void main(String[] args) throws Exception {

		PrintingBugReporter reporter = new PrintingBugReporter();
		PrintingCommandLine commandLine = reporter.new PrintingCommandLine();

		int argCount = commandLine.parse(args, 0, 2, "Usage: " + PrintingCommandLine.class.getName()
				+ " [options] [<xml results> [<test results]] ");

		// Load plugins, in order to get message files
		DetectorFactoryCollection.instance();
		
		if (reporter.stylesheet != null) {
			// actually do xsl via HTMLBugReporter instead of PrintingBugReporter
			xslt(reporter.stylesheet, args, argCount);
			return;
		}
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		if (argCount < args.length)
			bugCollection.readXML(args[argCount++], new Project());
		else
			bugCollection.readXML(System.in, new Project());
		
		if (argCount < args.length)
			reporter.setOutputStream(new PrintStream(new FileOutputStream(args[argCount++]), true));
		RuntimeException storedException = null;
		if (reporter.annotationUploadFormat) {
			bugCollection.computeBugHashes();
			for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
				BugInstance warning = i.next();
				try {
					String fHash = "fb-"+ 	warning.getInstanceHash() +"-"+	warning.getInstanceOccurrenceNum()
					+"-"+warning.getInstanceOccurrenceMax();
					
					
				System.out.print("#" + fHash);
				String key = warning.getUserDesignationKey();
				if (key.equals(BugDesignation.UNCLASSIFIED) || key.equals("NEEDS_FURTHER_STUDY"))
					System.out.print("#-1#"+key);
				else if (key.equals("MUST_FIX") || key.equals("SHOULD_FIX"))
					System.out.print("#7#"+key);
				else System.out.print("#0#"+key);
				SourceLineAnnotation sourceLine = warning.getPrimarySourceLineAnnotation();
				if (sourceLine != null) 
					System.out.println("#" + sourceLine.getSourceFile() + "#"+sourceLine.getStartLine());
				else System.out.println("##");
				System.out.println(warning.getAnnotationText());
				} catch (RuntimeException e) {
					if (storedException == null) 
					storedException = e;
				}
			}
		}
		else {
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
			BugInstance warning = i.next();
			try {
			reporter.printBug(warning);
			} catch (RuntimeException e) {
				if (storedException == null) 
				storedException = e;
			}
		}
		}
		if (storedException != null) throw storedException;
		
	}


	public static void xslt(String stylesheet, String[] args, int argCount) throws Exception {
		Project proj = new Project();
		HTMLBugReporter reporter = new HTMLBugReporter(proj, stylesheet);
		BugCollection bugCollection = reporter.getBugCollection();

		if (argCount < args.length) {
			proj.setProjectFileName(args[argCount]);
			bugCollection.readXML(args[argCount++], proj);
		} else
			bugCollection.readXML(System.in, new Project());

		if (argCount < args.length)
			reporter.setOutputStream(new PrintStream(new FileOutputStream(args[argCount++]), true));

		reporter.finish();
	}
}

// vim:ts=4
