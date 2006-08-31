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
package edu.umd.cs.findbugs.gui;

import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.TextUIBugReporter;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * BugReporter used by AnalysisRun.
 */
public class SwingGUIBugReporter extends TextUIBugReporter {
	private final AnalysisRun analysisRun;
	private SortedBugCollection bugCollection;
	private AnalysisErrorDialog errorDialog;
	private int errorCount;
	
	/**
	 * Constructor.
	 * 
	 * @param analysisRun
	 */
	public SwingGUIBugReporter(AnalysisRun analysisRun) {
		this.analysisRun = analysisRun;
		this.bugCollection = new SortedBugCollection(getProjectStats());
	}
	
	public SortedBugCollection getBugCollection() {
		return bugCollection;
	}
	
	public boolean errorsOccurred() {
		return errorCount > 0;
	}
	
	public AnalysisErrorDialog getErrorDialog() {
		return errorDialog;
	}
	
	public void observeClass(ClassDescriptor classDescriptor) {
	}
	
	@Override
	public void reportMissingClass(ClassNotFoundException ex) {
		++errorCount;
		super.reportMissingClass(ex);
		String message = getMissingClassName(ex);
		bugCollection.addMissingClass(message);
	}
	
	@Override
	public void logError(String message) {
		++errorCount;
		analysisRun.getFrame().getLogger().logMessage(ConsoleLogger.WARNING, message);
		super.logError(message);
		bugCollection.addError(message);
	}
	
	public void finish() {
	}
	
	@Override
	public void doReportBug(edu.umd.cs.findbugs.BugInstance bugInstance) {
		checkBugInstance(bugInstance);
		if (bugCollection.add(bugInstance))
			notifyObservers(bugInstance);
	}

	private void createDialog() {
		if (errorDialog == null) {
			errorDialog = new AnalysisErrorDialog(analysisRun.getFrame(), true, this);
		}
	}
	
	//@Override
	@Override
	public void reportQueuedErrors() {
		createDialog();
		errorDialog.clear();
		super.reportQueuedErrors();
		errorDialog.finish();
	}
	
	//@Override
	@Override
	protected void emitLine(String line) {
		line = line.replaceAll("\t", "  ");
		errorDialog.addLine(line);
	}
}
