package edu.umd.cs.findbugs.bluej;

import java.io.IOException;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

public class RunFindbugs {
	
	/**
	 * Returns a SortBugCollection of bugs that are in the classes that
	 * are passed to it.
	 * @param classes String[] of classes in the project
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static SortedBugCollection getBugs(String[] classes) throws IOException, InterruptedException {
		Project findBugsProject = new Project();
		for(String f : classes)
			findBugsProject.addFile(f);
		final SortedBugCollection bugs = new SortedBugCollection();
		BugReporter reporter = new MyReporter(bugs);
        System.setProperty("findbugs.jaws", "true");
        // System.out.println(System.getProperty("findbugs.jaws"));
        DetectorFactoryCollection.instance().ensureLoaded();
        
      
		IFindBugsEngine findBugs = new FindBugs2();
        findBugs.setBugReporter(reporter);
        findBugs.setProject(findBugsProject);
        findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
//      Honor current UserPreferences
        findBugs.setUserPreferences(UserPreferences.getUserPreferences());
        
		reporter.setPriorityThreshold(Detector.NORMAL_PRIORITY);
		
		// System.setProperty("findbugs.home", "/Users/pugh/Documents/eclipse-3.2/workspace/findbugs");

		
		findBugs.execute();
		
		return bugs;		
	}
}
