package edu.umd.cs.findbugs.bluej;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.SortedBugCollection;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.MenuGenerator;

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
							ArrayList<String> classes = new ArrayList<String>();
							for (BPackage bp : pckg.getProject().getPackages())
								for (BClass bc : bp.getClasses())
									classes.add(bc.getClassFile().getAbsolutePath());

							String[] classesArray = new String[classes.size()];
							classesArray = classes.toArray(classesArray);

							final SortedBugCollection bugs = RunFindbugs.getBugs(classesArray);
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									new ResultsFrame(bugs).setVisible(true);
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
}
