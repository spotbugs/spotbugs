package edu.umd.cs.findbugs.bluej;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.SortedBugCollection;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ExtensionException;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Adds our menu entry to the BlueJ tools menu.
 */
public class RegularMenuBuilder extends MenuGenerator
{
	private BlueJ bluej;
	
	public RegularMenuBuilder(BlueJ bluej)
	{
		this.bluej = bluej;
	}
	
	@SuppressWarnings("serial")
	@Override
	public JMenuItem getToolsMenuItem(final BPackage pckg)
	{
		JMenuItem jmi = new JMenuItem(new AbstractAction()
		{
			public void actionPerformed(ActionEvent evt)
			{				
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
								// At least one class in the project is not compiled.
								int response;
								String strButton = bluej.getExtensionPropertyString(FindBugsPreferences.PROFILE_LABEL,"");
														
								if(strButton.equals(FindBugsPreferences.radioCommand[0]))
									response = JOptionPane.YES_OPTION;
								else if(strButton.equals(FindBugsPreferences.radioCommand[1]))
									response = JOptionPane.NO_OPTION;
								else{
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
		});
		jmi.setText("Run FindBugs");
		return jmi;
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
					ResultsFrame.getInstance(bluej.getCurrentPackage().getProject(), true).update(bugs, bluej.getCurrentPackage().getProject());
				}
				catch (Exception e)
				{
					Log.recordBug(e);
				}
			}
		});
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
}
