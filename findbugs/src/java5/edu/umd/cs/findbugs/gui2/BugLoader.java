/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugReporterObserver;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.workflow.Update;

/**
 * Everything having to do with loading bugs should end up here.
 * @author Dan
 *
 */
public class BugLoader {

	private static Project loadedProject;
	
	/**
	 * This is how we attach to a FindBugs analysis, the PrintCallBack ignores most things FindBugs returns to us, implementing things like showing what classes were missing might be done here.
	 * This just adds all the bugs FindBugs finds to our BugSet.  
	 * @author Dan
	 *
	 */
	private static class PrintCallBack implements edu.umd.cs.findbugs.BugReporter
	{
		FindBugs fb;
		ArrayList<BugLeafNode> bugs; 
		ArrayList<BugReporterObserver> bros;
		ArrayList<String> errorLog;
		boolean done;
		int priorityThreadhold = Priorities.LOW_PRIORITY;
		
		PrintCallBack()
		{
			bros=new ArrayList<BugReporterObserver>();
			errorLog=new ArrayList<String>();
			bugs=new ArrayList<BugLeafNode>();
			done=false;
		}
		
		public void setEngine(FindBugs engine) {
			if (done==true)
			{
				throw new IllegalStateException("This PrintCallBack has already finished its analysis, create a new callback if you want another analysis done");
			}
			if (fb!=null)
			{
				throw new IllegalStateException("The engine has already been set, but the analysis was not finished, create a new callback instead of running multiple analyses from this one");
			}
			fb=engine;
//			System.out.println("Engine Set!");
		}

		public void setErrorVerbosity(int level) {
//			System.out.println("Error Verbosity set at " + level);
		}

		public void setPriorityThreshold(int threshold) {
			priorityThreadhold = Math.min(Priorities.EXP_PRIORITY, threshold);
		}

		public void reportBug(BugInstance bugInstance) {
			if (bugInstance.getPriority() > priorityThreadhold) return;
			BugLeafNode b = new BugLeafNode(bugInstance);
			bugs.add(b);
		}

		public void finish() {
//			System.out.println("All Done!");
			for (String s: errorLog)
			{
				System.err.println(s);
			}
			done=true;
		}

		public void reportQueuedErrors() {
//			System.out.println("We are supposed to report queued errors, whatever that means.");
//			System.out.println(errorLog);
		}

		public void addObserver(BugReporterObserver observer) {
			bros.add(observer);
		}

		public ProjectStats getProjectStats() {
//			System.out.println("Oh noes, look out for null pointers!");
			return null;
		}

		public BugReporter getRealBugReporter() {
			return this;
		}

		public void reportMissingClass(ClassNotFoundException ex) {
			Debug.println(ex);
		}

		public void reportSkippedAnalysis(JavaClassAndMethod method) {
//			System.out.println("we skipped something" + method);
		}

		public void logError(String message) {
//			System.out.println("logging an error");
			errorLog.add(message);
		}

		public void logError(String message, Throwable e) {
			if (e instanceof MissingClassException) {
				reportMissingClass(((MissingClassException)e).getClassNotFoundException());
				return;
			}
//			System.out.println("logging an error with stack trace");
//			e.printStackTrace();
			errorLog.add(message);
		}

		public void observeClass(ClassDescriptor classDescriptor) {
//			System.out.println("I am now observing ClassDescriptor " + classDescriptor);
		}

		public BugSet getBugs()
		{
			return new BugSet(bugs);
		}

		public void reportSkippedAnalysis(MethodDescriptor method) {
//			System.out.println("method " + method.getMethodName() + " of class " + method.getClassName() + " was skipped");
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
		 */
		public void reportMissingClass(ClassDescriptor classDescriptor) {
			// TODO Auto-generated method stub
			
		}

		
	}
	
	/**
	 * Performs an analysis and returns the BugSet created
	 * 
	 * @param p The Project to run the analysis on
	 * @param progressCallback the progressCallBack is supposed to be supplied by analyzing dialog, FindBugs supplies progress information while it runs the analysis
	 * @return the bugs found 
	 */
	public static BugSet doAnalysis(@NonNull Project p, FindBugsProgress progressCallback)
	{
		PrintCallBack pcb=new PrintCallBack();
		IFindBugsEngine fb=createEngine(p, pcb);
		fb.setUserPreferences(UserPreferences.getUserPreferences());
		fb.setProgressCallback(progressCallback);
		try {
			fb.execute();
			List<String> possibleDirectories=p.getSourceDirList();
//			System.out.println("List of directories: "+p.getSourceDirList());
			MainFrame.getInstance().setSourceFinder(new SourceFinder());
			MainFrame.getInstance().getSourceFinder().setSourceBaseList(possibleDirectories);

			return pcb.getBugs();
		} catch (IOException e) {
			Debug.println(e);
		} catch (InterruptedException e) {
			// Do nothing
		}
		
		
		return null;
		
	}

	/**
	 * Create the IFindBugsEngine that will be used to
	 * analyze the application.
	 * 
	 * @param p   the Project
	 * @param pcb the PrintCallBack
	 * @return the IFindBugsEngine
	 */
	private static IFindBugsEngine createEngine(@NonNull Project p, PrintCallBack pcb)
	{
		if (false) {
			// Old driver - don't use
			return new FindBugs(pcb, p);
		} else {
			FindBugs2 engine = new FindBugs2();
			engine.setBugReporter(pcb);
			engine.setProject(p);
			
			engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
			
			return engine;
		}
	}
	
	public static BugSet loadBugsHelper(BugCollection collection)
	{
		ArrayList<BugLeafNode> bugList=new ArrayList<BugLeafNode>();
		Iterator<BugInstance> i=collection.iterator();
		while (i.hasNext())
		{
			bugList.add(new BugLeafNode(i.next()));
		}
		
		return new BugSet(bugList);
		
	}
	
	public static BugSet loadBugs(Project project, URL url){
		try {
			return loadBugs(project, url.openConnection().getInputStream());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,"This file contains no bug data");
		}	
		return null;
	}
	public static BugSet loadBugs(Project project, File file){
		try {
			return loadBugs(project, new FileInputStream(file));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,"This file contains no bug data");
		}	
		return null;
	}
	public static BugSet loadBugs(Project project, InputStream in)
	{
			try 
			{
				SortedBugCollection col=new SortedBugCollection();
				col.readXML(in, project);
				List<String> possibleDirectories=project.getSourceDirList();
				MainFrame.getInstance().setSourceFinder(new SourceFinder());
				MainFrame.getInstance().getSourceFinder().setSourceBaseList(possibleDirectories);

				return loadBugsHelper(col);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,"This file contains no bug data");
			} catch (DocumentException e) {
				JOptionPane.showMessageDialog(null,"This file does not have the correct format; it may be corrupt.");
			}
			return null;
	}


	public static Project getLoadedProject()
	{
		return loadedProject;
	}
	
	private BugLoader()
	{
		//NO.
	}
	
	/**
	 * TODO: This really needs to be rewritten such that they don't have to choose ALL xmls in one fel swoop.  I'm thinking something more like new project wizard's functionality. -Dan
	 * 
	 * Merges bug collection histories from xmls selected by the user.  Right now all xmls must be in the same folder and he must select all of them at once
	 * Makes use of FindBugs's mergeCollection method in the Update class of the workflow package
	 * @return the merged collecction of bugs
	 */
	public static BugSet combineBugHistories()
	{
		try
		{
			FBFileChooser chooser=new FBFileChooser();
			chooser.setFileFilter(new FindBugsAnalysisFileFilter());
//			chooser.setCurrentDirectory(GUISaveState.getInstance().getStarterDirectoryForLoadBugs());  This is done by FBFileChooser.
			chooser.setMultiSelectionEnabled(true);
			chooser.setDialogTitle("Choose All XML's To Combine");
			if (chooser.showOpenDialog(MainFrame.getInstance())==JFileChooser.CANCEL_OPTION)
				return null;
			
			loadedProject=new Project();
			SortedBugCollection conglomeration=new SortedBugCollection();
			conglomeration.readXML(chooser.getSelectedFiles()[0],loadedProject);
			Update update = new Update();
			for (int x=1; x<chooser.getSelectedFiles().length;x++)
			{
				File f=chooser.getSelectedFiles()[x];
				Project p=new Project();
				SortedBugCollection col=new SortedBugCollection();
				col.readXML(f,p);
				conglomeration=(SortedBugCollection) update.mergeCollections(conglomeration, col, false);//False means dont show dead bugs
			}
			
			return loadBugsHelper(conglomeration);
		} catch (IOException e) {
			Debug.println(e);
			return null;
		} catch (DocumentException e) {
			Debug.println(e);
			return null;
		}

	}
	
	/**
	 * Does what it says it does, hit apple r (control r on pc) and the analysis is redone using the current project
	 * @param p
	 * @return the bugs from the reanalysis, or null if cancelled
	 */
	public static BugSet redoAnalysisKeepComments(Project p)
	{
		BugSet oldSet=BugSet.getMainBugSet();
		SortedBugCollection current=new SortedBugCollection();
		
		for (BugLeafNode node: oldSet)
		{
			BugInstance bug=node.getBug();
			current.add(bug);
		}
		Update update = new Update();
		final SortedBugCollection justAnalyzed=new SortedBugCollection();
		
		RedoAnalysisCallback ac= new RedoAnalysisCallback(justAnalyzed);
		
		new AnalyzingDialog(p,ac,true);
		
		if (ac.finished)
			return loadBugsHelper(update.mergeCollections(current, justAnalyzed, false));
		else
			return null;
		
	}
	
	/** just used to know how the new analysis went*/
	private static class RedoAnalysisCallback implements AnalysisCallback
	{
		public RedoAnalysisCallback(SortedBugCollection collection)
		{
			justAnalyzed=collection;
		}
		
		SortedBugCollection justAnalyzed;
		boolean finished;
		public void analysisFinished(BugSet b)
		{
			for(BugLeafNode node: b)
			{
				BugInstance bug=node.getBug();
				justAnalyzed.add(bug);
			}
			finished=true;
		}

		public void analysisInterrupted() 
		{
			finished=false;
		}
	}
}