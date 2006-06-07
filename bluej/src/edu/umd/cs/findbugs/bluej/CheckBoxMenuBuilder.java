package edu.umd.cs.findbugs.bluej;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
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

	private static Map<BProject, Boolean> isRunningMap = new HashMap<BProject, Boolean>();
	private static Map<BProject, HashSet<JCheckBoxMenuItem>> projectToMenuItem = new HashMap<BProject, HashSet<JCheckBoxMenuItem>>();
	
	private static final boolean DEFAULT_RUNNING = false;
	
	public CheckBoxMenuBuilder(BlueJ bluej)
	{
		this.bluej = bluej;
	}

	public static boolean isRunning(BProject project)
	{
		if (!isRunningMap.containsKey(project))
			return DEFAULT_RUNNING;
		return isRunningMap.get(project);
	}
	
	/**
	 * @see bluej.extensions.MenuGenerator#getToolsMenuItem(bluej.extensions.BPackage)
	 */
	@Override
	public JMenuItem getToolsMenuItem(BPackage pckg)
	{
		try
		{
			JCheckBoxMenuItem result = new JCheckBoxMenuItem("Run FindBugs on Compile");
			result.addActionListener(new CheckBoxMenuAction(pckg.getProject(), result));
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
	/*@Override
	public void notifyPostToolsMenu(BPackage pckg, JMenuItem menu)
	{
		try
		{
			BProject project = pckg.getProject();
			if (!isRunningMap.containsKey(project))
				isRunningMap.put(project, DEFAULT_RUNNING);
			menu.setSelected(isRunningMap.get(project));
		}
		// Checked exception will never occur, since the 
		// package (and therefore the project) must be open.
		catch (ProjectNotOpenException e)
		{
			Log.recordBug(e);
		}
	*/
	
	@SuppressWarnings("serial")
	class CheckBoxMenuAction extends AbstractAction
	{
		private BProject project;
		
		public CheckBoxMenuAction(BProject project, JCheckBoxMenuItem menuItem)
		{
			this.project = project;
			if(!projectToMenuItem.containsKey(project))
				projectToMenuItem.put(project, new HashSet<JCheckBoxMenuItem>());
			
			menuItem.setSelected(isRunning(project));			
			projectToMenuItem.get(project).add(menuItem);
		}
		
		/**
		 * Clicked. Update our records as to whether it's checked or not.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			isRunningMap.put(project, ((JCheckBoxMenuItem)evt.getSource()).isSelected());
			HashSet<JCheckBoxMenuItem> projectItems = projectToMenuItem.get(project);
			for(JCheckBoxMenuItem mItem : projectItems){
				mItem.setSelected(isRunningMap.get(project));
			}
		}
	}
}
