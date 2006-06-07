package edu.umd.cs.findbugs.bluej;

import java.io.IOException;

import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;


public class RunFindbugs implements CompileListener {
	
	public static SortedBugCollection getBugs(String[] classes) throws IOException, InterruptedException {
		Project findBugsProject = new Project();
		for(String f : classes)
			findBugsProject.addFile(f);
		final SortedBugCollection bugs = new SortedBugCollection();
		BugReporter reporter = new MyReporter(bugs);
		
		FindBugs findBugs = new FindBugs(reporter, findBugsProject);
		reporter.setPriorityThreshold(Detector.NORMAL_PRIORITY);
	    
		
		System.setProperty("findbugs.home", "/Users/pugh/Documents/eclipse-3.2/workspace/findbugs");
		System.setProperty("findbugs.jaws", "true");
		
		findBugs.execute();
		
		return bugs;
		
/*		for(BugInstance bug : bugs.getCollection()) {
			SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();

			System.out.println(bug.getPrimarySourceLineAnnotation());
			System.out.println(bug.getMessageWithoutPrefix());
			System.out.println(bug.getBugPattern().getDetailHTML());
			
		}
*/
			
	}

	public void compileStarted(CompileEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	public void compileError(CompileEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	public void compileWarning(CompileEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	public void compileSucceeded(CompileEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	public void compileFailed(CompileEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

}
