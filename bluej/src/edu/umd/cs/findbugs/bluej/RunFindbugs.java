package edu.umd.cs.findbugs.bluej;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ExtensionException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;


public class RunFindbugs implements CompileListener {
	
	BlueJ bluej;
	
	public RunFindbugs(BlueJ bluej)
	{
		this.bluej = bluej;
	}
	
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

	private void getAllClassesAndRun() throws IOException, InterruptedException, ExtensionException
	{
		final SortedBugCollection bugs = RunFindbugs.getBugs(allClassFileNames(bluej.getCurrentPackage().getProject()));
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					ResultsFrame.getInstance().update(bugs, bluej.getCurrentPackage().getProject());
				}
				catch (Exception e)
				{
					Log.recordBug(e);
				}
			}
		});
	}
	
	/**
	 * Run FindBugs exactly as with compileSucceeded()
	 */
	public void compileWarning(CompileEvent evt)
	{
		compileSucceeded(evt);
	}

	/**
	 * Compile successfully completed, so run FindBugs.
	 */
	public void compileSucceeded(CompileEvent evt)
	{
		// First thing, make sure we're supposed to be running
		try
		{
			if (!CheckBoxMenuBuilder.isRunning(bluej.getCurrentPackage().getProject()))
				return;
		}
		catch (ExtensionException e)
		{
			Log.recordBug(e);
			return;
		}
		
		new Thread(new Runnable() {
			public void run()
			{
				// First see if all classes are compiled
				ArrayList<BClass> notCompiled = new ArrayList<BClass>();
				try
				{
					for (BPackage bp : bluej.getCurrentPackage().getProject().getPackages())
						for (BClass bc : bp.getClasses())
							if (!bc.isCompiled())
								notCompiled.add(bc);
					
					if (notCompiled.size() > 0)
					{
						int response;
						String strButton = bluej.getExtensionPropertyString(FindBugsPreferences.PROFILE_LABEL,"");
												
						if(strButton.equals(FindBugsPreferences.radioCommand[0]))
							response = JOptionPane.YES_OPTION;
						else if(strButton.equals(FindBugsPreferences.radioCommand[1]))
							response = JOptionPane.NO_OPTION;
						else{
							// At least one class in the project is not compiled.
							StringBuffer msg = new StringBuffer();
							msg.append("The following class" + (notCompiled.size() == 1 ? " is " : "es are ") + "not compiled:\n\n");
							for (BClass bc : notCompiled)
								msg.append(bc.getName() + "\n");
							msg.append("\nCompile before running FindBugs?");
							response = JOptionPane.showConfirmDialog(null, msg);
						}
						
						switch (response)
						{
						case JOptionPane.YES_OPTION:
							for (BPackage bp : bluej.getCurrentPackage().getProject().getPackages())
								bp.compile(true);
							break;
						case JOptionPane.NO_OPTION:
							// Don't do anything - get out of the switch and just run
							break;
						case JOptionPane.CANCEL_OPTION:
							return;
						}
					}

					getAllClassesAndRun();
				}
				catch (Exception e)
				{
					Log.recordBug(e);
				}
			}
		}).start();
	}
	
	/**
	 * @param BProject the current project
	 * @return Absolute paths for every .class file in the project
	 */
	private String[] allClassFileNames(BProject project) throws ProjectNotOpenException
	{
		ArrayList<String> classes = new ArrayList<String>();

		FilenameFilter isClassFile = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".class");
			}
		};
		for (BPackage bp : project.getPackages())
		{
			try
			{
				for (File f : bp.getDir().listFiles(isClassFile))
				{
					if (bp.getBClass(f.getName().substring(0, f.getName().length() - ".class".length())) != null)
						// The class found has a BClass, meaning it's the public top-level class for its source file
						// and it's still in the project.
						classes.add(f.getAbsolutePath());
					else
						if (f.getName().contains("$"))
						{
							// Not a top-level class: accept it if its top-level is still in the project,
							// and if they were compiled within 30 seconds of each other	
							String topLevelName = f.getName().substring(0, f.getName().indexOf("$"));
							if (bp.getBClass(topLevelName) != null && f.lastModified() >= bp.getBClass(topLevelName).getClassFile().lastModified() - 30000)
								classes.add(f.getAbsolutePath());
						}
				}
			}
			catch (PackageNotFoundException e)
			{
				Log.recordBug(e);
			}
		}
		String[] classesArray = new String[classes.size()];
		classesArray = classes.toArray(classesArray);
		return classesArray;
	}

	/** NOOP, but required for the interface */
	public void compileStarted(CompileEvent arg0) {}

	/** NOOP, but required for the interface */
	public void compileError(CompileEvent arg0) {}
	
	/** NOOP, but required for the interface */
	public void compileFailed(CompileEvent arg0) {}

}
