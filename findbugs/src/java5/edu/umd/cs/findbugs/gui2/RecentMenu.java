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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.umd.cs.findbugs.gui2.MainFrame.SaveType;

/**
 * @author Dan
 */


public class RecentMenu{

	private static class LimitedArrayList<T> extends ArrayList<T>
	{
		public static final int MAX_ENTRIES=5;

		public LimitedArrayList()
		{
			super(MAX_ENTRIES);
		}
		
		@Override
        public boolean add(T element)
		{
			if (!this.contains(element))
			{
				super.add(0,element);
				if (this.size()>MAX_ENTRIES)
				{
					this.remove(MAX_ENTRIES);
				}
			}
			else
			{
				this.remove(element);
				super.add(0,element);
			}
			return true;
		}
	}
	
	LimitedArrayList<File> recentProjects;
	LimitedArrayList<File> recentAnalyses;
	JMenu recentMenu;

	public RecentMenu(JMenu menu)
	{
		recentProjects=new LimitedArrayList<File>();
		recentAnalyses=new LimitedArrayList<File>();
		recentMenu=menu;
		recentMenu.add("Recent Projects:").setEnabled(false);
		
		for (File f: GUISaveState.getInstance().getRecentProjects())
		{
			recentProjects.add(f);
		}
		
		for (File f: GUISaveState.getInstance().getRecentAnalyses())
		{
			recentAnalyses.add(f);
		}
		
		makeRecentMenu();
	}

	public void makeRecentMenu()
	{
		recentMenu.removeAll();
		recentMenu.add("Recent Projects:").setEnabled(false);
		for (File f: recentProjects)
		{
			Debug.println(f);
			if (!f.exists())
			{
				if (MainFrame.DEBUG) System.err.println("a recent project was not found, removing it from menu");
				continue;
			}

			recentMenu.add(MainFrame.getInstance().createRecentItem(f, SaveType.PROJECT));
		}
		recentMenu.addSeparator();
		recentMenu.add("Recent Analyses:").setEnabled(false);
		for (File f: recentAnalyses)
		{
			Debug.println(f);
			if (!f.exists())
			{
				if (MainFrame.DEBUG) System.err.println("a analysis project was not found, removing it from menu");
				continue;
			}

			recentMenu.add(MainFrame.getInstance().createRecentItem(f, SaveType.XML_ANALYSIS));
		}
	}
	
	public void addRecentFile(final File f, final SaveType localSaveType)
	{
		if (localSaveType==SaveType.PROJECT)
			recentProjects.add(f);
		else if (localSaveType==SaveType.XML_ANALYSIS)
			recentAnalyses.add(f);
		makeRecentMenu();
	}
	
	

	
	
	
}
