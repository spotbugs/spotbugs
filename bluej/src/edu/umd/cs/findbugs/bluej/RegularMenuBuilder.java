package edu.umd.cs.findbugs.bluej;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.SortedBugCollection;

import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

public class RegularMenuBuilder extends MenuGenerator
{
	@Override
	public JMenuItem getToolsMenuItem(final BPackage pckg)
	{
		JMenuItem jmi = new JMenuItem(new AbstractAction()
		{
			public void actionPerformed(ActionEvent evt)
			{				
				new Thread(new Runnable()
				{
					public void run()
					{
						try
						{
							final SortedBugCollection bugs = RunFindbugs.getBugs(allClassFileNames(pckg.getProject()));
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									try
									{
										ResultsFrame.getInstance().update(bugs, pckg.getProject());
									}
									catch (Exception e)
									{
										Log.recordBug(e);
									}
								}
							});

						}
						catch (Exception e)
						{
							Log.recordBug(e);
						}
					}
				}).start();
			}
		});
		jmi.setText("Run FindBugs");
		return jmi;
	}
	
	/**
	 * @param BProject the current project
	 * @return Absolute paths for every .class file in the project
	 */
	private String[] allClassFileNames(BProject project) throws ProjectNotOpenException
	{
		ArrayList<String> classes = new ArrayList<String>();
//		for (BPackage bp : pckg.getProject().getPackages())
//			for (BClass bc : bp.getClasses())
//				classes.add(bc.getClassFile().getAbsolutePath());

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
}
