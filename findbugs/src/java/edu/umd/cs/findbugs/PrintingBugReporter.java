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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

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
			addSwitch("-rank", "list rank when generating text");
			addSwitch("-designations", "report user designations for each bug");
			addSwitch("-history", "report first and last versions for each bug");
			addSwitch("-applySuppression", "exclude any bugs that match suppression filters");
			addSwitch("-annotationUpload", "generate annotations in upload format");
			addSwitchWithOptionalExtraPart("-html", "stylesheet",
			"Generate HTML output (default stylesheet is default.xsl)");
			addOption("-pluginList", "jar1[" + File.pathSeparator + "jar2...]",
				  "specify list of plugin Jar files to load");
		}
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-longBugCodes"))
				setUseLongBugCodes(true);
			else if (option.equals("-rank"))
				setShowRank(true);
			else if (option.equals("-designations"))
				setReportUserDesignations(true);
			else if (option.equals("-applySuppression"))
				setApplySuppressions(true);
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
			if (option.equals("-pluginList")) {
				String pluginListStr = argument;
				ArrayList<URL> pluginList = new ArrayList<URL>();
				StringTokenizer tok = new StringTokenizer(pluginListStr, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					pluginList.add(new File(tok.nextToken()).toURL());
				}

				DetectorFactoryCollection.rawInstance().setPluginList(pluginList.toArray(new URL[pluginList.size()]));
			} else {
				throw new IllegalStateException();
			}
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
			xslt(reporter.stylesheet, reporter.isApplySuppressions(), args, argCount);
			return;
		}

		SortedBugCollection bugCollection = new SortedBugCollection();
		if (argCount < args.length)
			bugCollection.readXML(args[argCount++]);
		else
			bugCollection.readXML(System.in);

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
				else if (key.equals("MUST_FIX") || key.equals("SHOULD_FIX")  || key.equals("I_WILL_FIX"))
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
		for (BugInstance warning :  bugCollection.getCollection())
		    if (!reporter.isApplySuppressions() || !bugCollection.getProject().getSuppressionFilter().match(warning) ){
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


	public static void xslt(String stylesheet, boolean applySuppression, String[] args, int argCount) throws Exception {
		Project proj = new Project();
		HTMLBugReporter reporter = new HTMLBugReporter(proj, stylesheet);
		BugCollection bugCollection = reporter.getBugCollection();

		bugCollection.setApplySuppressions(applySuppression);
		if (argCount < args.length) {
			bugCollection.readXML(args[argCount++]);
		} else
			bugCollection.readXML(System.in);

		if (argCount < args.length)
			reporter.setOutputStream(new PrintStream(new FileOutputStream(args[argCount++]), true));

		reporter.finish();
		Exception e = reporter.getFatalException();
		if (e != null) throw e;
	}
}

// vim:ts=4
