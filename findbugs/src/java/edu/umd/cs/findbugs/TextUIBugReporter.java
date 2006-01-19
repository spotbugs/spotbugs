/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Base class for BugReporters which provides convenient formatting
 * and reporting of warnings and analysis errors.
 * 
 * <p>
 * "TextUIBugReporter" is a bit of a misnomer, since this class
 * is useful in GUIs, too.
 * </p>
 * 
 * @author David Hovemeyer
 */
public abstract class TextUIBugReporter extends AbstractBugReporter {
	private boolean reportStackTrace;
	private boolean useLongBugCodes = false;
	// Map of category codes to abbreviations used in printBug()
	private static final HashMap<String, String> categoryMap = new HashMap<String, String>();

	static {
		categoryMap.put("CORRECTNESS", "C "); // "C"orrectness
		categoryMap.put("MT_CORRECTNESS", "M "); // "M"ultithreaded correctness
		categoryMap.put("MALICIOUS_CODE", "V "); // malicious code "V"ulnerability
		categoryMap.put("PERFORMANCE", "P "); // "P"erformance
		categoryMap.put("STYLE", "S "); // "S"tyle
		categoryMap.put("I18N", "I "); // "I"nternationalization
	}

	protected PrintStream outputStream = System.out;
	
	public TextUIBugReporter() {
		reportStackTrace = true;
	}

	
	/**
	 * Set the PrintStream to write bug output to.
	 * 
	 * @param outputStream the PrintStream to write bug output to
	 */
	public void setOutputStream(PrintStream outputStream) {
		this.outputStream = outputStream;
	}
	
	/**
	 * Set whether or not stack traces should be reported in error output.
	 * 
	 * @param reportStackTrace true if stack traces should be reported, false if not
	 */
	public void setReportStackTrace(boolean reportStackTrace) {
		this.reportStackTrace = reportStackTrace;
	}

	/**
	 * Print bug in one-line format.
	 * 
	 * @param bugInstance the bug to print
	 */
	protected void printBug(BugInstance bugInstance) {
		switch (bugInstance.getPriority()) {
		case Detector.EXP_PRIORITY:
			outputStream.print("E ");
			break;
		case Detector.LOW_PRIORITY:
			outputStream.print("L ");
			break;
		case Detector.NORMAL_PRIORITY:
			outputStream.print("M ");
			break;
		case Detector.HIGH_PRIORITY:
			outputStream.print("H ");
			break;
		}

		BugPattern pattern = bugInstance.getBugPattern();
		if (pattern != null) {
			String categoryAbbrev = categoryMap.get(pattern.getCategory());
			if (categoryAbbrev != null)
				outputStream.print(categoryAbbrev);
		}

		if (useLongBugCodes) {
		outputStream.print(bugInstance.getType());
		outputStream.print(" ");
		}
		SourceLineAnnotation line =
		        bugInstance.getPrimarySourceLineAnnotation();
		if (line == null)
			outputStream.println(bugInstance.getMessage());
		else
			outputStream.println(bugInstance.getMessage()
			        + "  " + line.toString());
	}

	private boolean analysisErrors;
	private boolean missingClasses;
	
	//@Override
	public void reportQueuedErrors() {
		analysisErrors = missingClasses = false;
		super.reportQueuedErrors();
	}
	
	public void reportAnalysisError(AnalysisError error) {
		if (!analysisErrors) {
			emitLine("The following errors occurred during analysis:");
			analysisErrors = true;
		}
		emitLine("\t" + error.getMessage());
		if (error.getExceptionMessage() != null) {
			emitLine("\t\t" + error.getExceptionMessage());
			if (reportStackTrace) {
				String[] stackTrace = error.getStackTrace();
				if (stackTrace != null) {
					for (String aStackTrace : stackTrace) {
						emitLine("\t\t\tAt " + aStackTrace);
					}
				}
			}
		}
	}

	public void reportMissingClass(String message) {
		if (!missingClasses) {
			emitLine("The following classes needed for analysis were missing:");
			missingClasses = true;
		}
		emitLine("\t" + message);
	}
	
	/**
	 * Emit one line of the error message report.
	 * By default, error messages are printed to System.err.
	 * Subclasses may override.
	 * 
	 * @param line one line of the error report
	 */
	protected void emitLine(String line) {
		line = line.replaceAll("\t", "  ");
		System.err.println(line);
	}


	public boolean getUseLongBugCodes() {
		return useLongBugCodes;
	}


	public void setUseLongBugCodes(boolean useLongBugCodes) {
		this.useLongBugCodes = useLongBugCodes;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
	 */
	public BugReporter getRealBugReporter() {
		return this;
	}
	
	/**
	 * For debugging: check a BugInstance to make sure it
	 * is valid.
	 * 
	 * @param bugInstance the BugInstance to check
	 */
	protected void checkBugInstance(BugInstance bugInstance) {
		for (Iterator<BugAnnotation> i = bugInstance.annotationIterator(); i.hasNext();) {
			BugAnnotation bugAnnotation = i.next();
			if (bugAnnotation instanceof PackageMemberAnnotation) {
				PackageMemberAnnotation pkgMember = (PackageMemberAnnotation) bugAnnotation;
				if (pkgMember.getSourceLines() == null) {
					throw new IllegalStateException("Package member " + pkgMember +
							" reported without source lines!");
				}
			}
		}
	}

}

// vim:ts=4
