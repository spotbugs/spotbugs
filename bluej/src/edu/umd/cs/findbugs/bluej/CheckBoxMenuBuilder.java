package edu.umd.cs.findbugs.bluej;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ExtensionException;
import bluej.extensions.MenuGenerator;
import bluej.extensions.ProjectNotOpenException;

public class CheckBoxMenuBuilder extends MenuGenerator
{
	private BlueJ bluej;

	private static Map<BProject, Boolean> isRunning = new HashMap<BProject, Boolean>();
	
	private static final boolean DEFAULT_RUNNING = false;
	
	public CheckBoxMenuBuilder(BlueJ bluej)
	{
		this.bluej = bluej;
	}

	public static boolean isRunning(BProject project)
	{
		if (!isRunning.containsKey(project))
			return DEFAULT_RUNNING;
		return isRunning.get(project);
	}
	
	/**
	 * @see bluej.extensions.MenuGenerator#getToolsMenuItem(bluej.extensions.BPackage)
	 */
	@Override
	public JMenuItem getToolsMenuItem(BPackage pckg)
	{
		//JCheckBoxMenuItem result = new JCheckBoxMenuItem("CheckBox Something");
		try
		{
			JCheckBoxMenuItem result = new JCheckBoxMenuItem(new CheckBoxMenuAction(pckg.getProject()));
			result.setText("CheckBox Something");
			return result;
		}
		catch (ProjectNotOpenException e)
		{
			Log.recordBug(e);
			return null;
		}
	}

	/**
	 * Called when the menu is about to be displayed.  If and only if we're enabled on
	 * this project, make sure the menu item is checked.  (If we've never seen this project
	 * before, start it off checked.) 
	 */
	@Override
	public void notifyPostToolsMenu(BPackage pckg, JMenuItem menu)
	{
		try
		{
			BProject project = pckg.getProject();
			if (!isRunning.containsKey(project))
				isRunning.put(project, DEFAULT_RUNNING);
			menu.setSelected(isRunning.get(project));
		}
		// Checked exception will never occur, since the 
		// package (and therefore the project) must be open.
		catch (ProjectNotOpenException e)
		{
			Log.recordBug(e);
		}
	}
	
	@SuppressWarnings("serial")
	class CheckBoxMenuAction extends AbstractAction
	{
		private BProject project;
		
		public CheckBoxMenuAction(BProject project)
		{
			this.project = project;
		}
		
		/**
		 * Clicked. Update our records as to whether it's checked or not.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			isRunning.put(project, ((JCheckBoxMenuItem)evt.getSource()).isSelected());
		}
	}
}
