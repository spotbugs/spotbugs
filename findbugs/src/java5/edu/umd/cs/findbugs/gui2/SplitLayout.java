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

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/**
 * @author pugh
 */
public class SplitLayout implements FindBugsLayoutManager {

	final MainFrame frame;
	JLabel sourceTitle;
	
	/**
	 * @param frame
	 */
	public SplitLayout(MainFrame frame) {
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

		JSplitPane topLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
				frame.bugListPanel(), frame.commentsPanel());
		topLeft.setOneTouchExpandable(true);
		topLeft.setDividerLocation(150); //0.65);
		
		JPanel sourcePanel = new JPanel();
		sourcePanel.setLayout(new BorderLayout());
		sourceTitle = new JLabel();
		sourceTitle.setText("<source listing>");
		sourcePanel.add(sourceTitle, BorderLayout.NORTH);
		sourcePanel.add(frame.createSourceCodePanel(), BorderLayout.CENTER);
		JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				topLeft, sourcePanel
				);
		top.setOneTouchExpandable(true);
		//topLeft.setDividerLocation(0.25); //default behaviour seems ok

		JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
				top,  frame.summaryTab());
		main.setOneTouchExpandable(true);
		main.setDividerLocation(300); //0.75);

		frame.setLayout(new BorderLayout());
		frame.add(main, BorderLayout.CENTER);
		frame.add(frame.statusBar(), BorderLayout.SOUTH);

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#makeCommentsVisible()
	 */
	public void makeCommentsVisible() {

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#makeSourceVisible()
	 */
	public void makeSourceVisible() {

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
		sourceTitle.setText(title);
	}
	
}
