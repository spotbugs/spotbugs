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

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/**
 * @author pugh
 */
public class TabbedLayout implements FindBugsLayoutManager {

	final MainFrame frame;
	
	private JTabbedPane mainTabs = null;
	/**
	 * @param frame
	 */
	public TabbedLayout(MainFrame frame) {
		this.frame = frame;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#createWindowMenu()
	 */
	public JMenu createWindowMenu() {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#initialize()
	 */
	public void initialize() {
		//This will change the look of the tabs and filechoosr if the font size is set to greater than 15.
		if(Driver.getFontSize() > 15 && System.getProperty("os.name").startsWith("Mac")){
			UIManager.put("TabbedPaneUI", "javax.swing.plaf.basic.BasicTabbedPaneUI");
			UIManager.put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
		}
		
		mainTabs = bottomTabs();
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				frame.bugListPanel(), mainTabs);
		split.setOneTouchExpandable(true);
		split.setDividerLocation(275);

		frame.setLayout(new BorderLayout());
		frame.add(split, BorderLayout.CENTER);
		frame.add(frame.statusBar(), BorderLayout.SOUTH);

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#makeCommentsVisible()
	 */
	public void makeCommentsVisible() {
		mainTabs.setSelectedIndex(1);

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#makeSourceVisible()
	 */
	public void makeSourceVisible() {
		mainTabs.setSelectedIndex(2);

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#saveState()
	 */
	public void saveState() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#setSourceTitle(java.lang.String)
	 */
	public void setSourceTitle(String title) {
		mainTabs.setTitleAt(2, title);

	}
	/**
	 * Creates the bottom tabs of the GUI.
	 * @return
	 */
	JTabbedPane bottomTabs()
	{
		JTabbedPane bottomTabs = new JTabbedPane();
		
		bottomTabs.addTab("Bug Summary", frame.summaryTab());
		bottomTabs.addTab("Comments", null, frame.commentsPanel(), 
				"User defined comments of current bug.");
		bottomTabs.addTab("Source", null, frame.createSourceCodePanel(),
				"Source code of current bug if available.");
		
		//Set keyboard mnemonic for tabs.
		bottomTabs.setMnemonicAt(0, KeyEvent.VK_B);
		bottomTabs.setMnemonicAt(1, KeyEvent.VK_C);
		bottomTabs.setMnemonicAt(2, KeyEvent.VK_S);
		
		return bottomTabs;
	}
}
