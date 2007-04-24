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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import net.infonode.docking.DockingWindow;
import net.infonode.docking.DockingWindowAdapter;
import net.infonode.docking.DockingWindowListener;
import net.infonode.docking.RootWindow;
import net.infonode.docking.SplitWindow;
import net.infonode.docking.TabWindow;
import net.infonode.docking.View;
import net.infonode.docking.theme.DockingWindowsTheme;
import net.infonode.docking.theme.ShapedGradientDockingTheme;
import net.infonode.docking.title.DockingWindowTitleProvider;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.ViewMap;

/**
 * @author pugh
 */
public class DockLayout implements FindBugsLayoutManager {
	private static class DockParentListener extends DockingWindowAdapter
	{
		@Override
		public void windowClosed(DockingWindow window)
		{
			// Notify all children's listeners
			ArrayList<DockingWindow> children = new ArrayList<DockingWindow>();
			for (int i = 0; i < window.getChildWindowCount(); i++)
				children.add(window.getChildWindow(i));
			for (DockingWindow i : children)
				i.close();
		}
	}
	private class ViewMenuItem extends JCheckBoxMenuItem implements ItemListener
	{
		private View view;

		public ViewMenuItem(View view, String title)
		{
			super(title, true);
			addItemListener(this);
			this.view = view;
//			view.addListener(new Listener());
		}

		// Menu item has been checked or unchecked
		public void itemStateChanged(ItemEvent evt)
		{
			if (evt.getStateChange() == ItemEvent.SELECTED)
				DockingUtil.addWindow(view, rootWindow);
			if (evt.getStateChange() == ItemEvent.DESELECTED)
				view.close();
		}

//		private class Listener extends DockingWindowAdapter
//		{
//			@Override
//			public void windowAdded(DockingWindow addedToWindow, DockingWindow addedWindow)
//			{
//				if (addedWindow.equals(view))
//					ViewMenuItem.this.setSelected(true);
//			}
//			
//			@Override
//			public void windowRemoved(DockingWindow removedFromWindow, DockingWindow removedWindow)
//			{
//				if (removedWindow.equals(view))
//					ViewMenuItem.this.setSelected(false);
//			}
//		}
	}
	private View commentsView = null;
	final MainFrame frame;
	private RootWindow rootWindow;
	private View sourceView = null;
	private View summaryView = null;
	private TabWindow tabs = null;

	private View topView = null;
	private Map<View, ViewMenuItem> viewMenuItems = null;
	/**
	 * @param frame
	 */
	public DockLayout(MainFrame frame) {
		this.frame = frame;
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#createWindowMenu()
	 */
	public JMenu createWindowMenu() {

		viewMenuItems = new HashMap<View, ViewMenuItem>();
		viewMenuItems.put(summaryView, new ViewMenuItem(summaryView, "Bug summary"));
		viewMenuItems.put(commentsView, new ViewMenuItem(commentsView, "Comments"));
		viewMenuItems.put(sourceView, new ViewMenuItem(sourceView, "Source code"));

		JMenu windowMenu = new JMenu("Window");
		windowMenu.setMnemonic(KeyEvent.VK_W);
		windowMenu.add(viewMenuItems.get(summaryView));
		windowMenu.add(viewMenuItems.get(commentsView));
		windowMenu.add(viewMenuItems.get(sourceView));
		return windowMenu;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#initialize()
	 */
	public void initialize() {
		ViewMap viewMap = new ViewMap();
		topView = new View(L10N.getLocalString("view.bugs", "Bugs"), null, frame.bugListPanel());
		topView.getWindowProperties().setCloseEnabled(false);
		viewMap.addView(0, topView);
		summaryView = new View(L10N.getLocalString("view.bug_summary", "Bug Summary"), null, frame.summaryTab());
		viewMap.addView(1, summaryView);
		commentsView = new View(L10N.getLocalString("view.comments", "Comments"), null, frame.commentsPanel());
		viewMap.addView(2, commentsView);
		sourceView = new View(L10N.getLocalString("view.source", "Source"), null, frame.createSourceCodePanel());
		viewMap.addView(3, sourceView);

		rootWindow = DockingUtil.createRootWindow(viewMap, true);

		tabs = new TabWindow(new DockingWindow[]{summaryView, commentsView, sourceView});
		tabs.addListener(new DockParentListener());
		tabs.setSelectedTab(0);
//		tabs.getWindowProperties().setCloseEnabled(false);

		rootWindow.setWindow(new SplitWindow(false, 0.4f, topView, tabs));

		DockingWindowsTheme theme = new ShapedGradientDockingTheme();
		rootWindow.getRootWindowProperties().addSuperObject(theme.getRootWindowProperties());

		try
		{
			rootWindow.read(new ObjectInputStream(new ByteArrayInputStream(GUISaveState.getInstance().getDockingLayout())), true);
		}
		catch (IOException e) {}

		DockingWindowListener listener = new DockingWindowAdapter()
		{
			@Override
			public void windowAdded(DockingWindow addedToWindow, DockingWindow addedWindow)
			{
				viewMenuItems.get(addedWindow).setSelected(true);

				addedToWindow.addListener(new DockParentListener());
			}

			@Override
			public void windowClosed(DockingWindow window)
			{
				viewMenuItems.get(window).setSelected(false);
			}
		};

		summaryView.addListener(listener);
		commentsView.addListener(listener);
		sourceView.addListener(listener);

		frame.setLayout(new BorderLayout());
		frame.add(rootWindow, BorderLayout.CENTER);
		frame.add(frame.statusBar(), BorderLayout.SOUTH);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#makeCommentsVisible()
	 */
	public void makeCommentsVisible() {
		commentsView.makeVisible();

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#makeSourceVisible()
	 */
	public void makeSourceVisible() {
		sourceView.makeVisible();

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#saveState()
	 */
	public void saveState() {
		try
		{
			// FIXME this is writing the wrong array and I don't know why
			ByteArrayOutputStream dockingLayout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(dockingLayout);
			rootWindow.write(out, true);
			out.close();
			GUISaveState.getInstance().setDockingLayout(dockingLayout.toByteArray());
		}
		catch (IOException e) {}

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.gui2.LayoutManager#setSourceTitle(java.lang.String)
	 */
	public void setSourceTitle(final String title) {
	sourceView.getWindowProperties().setTitleProvider(new DockingWindowTitleProvider(){
		public String getTitle(DockingWindow arg0) {
			return title;
		}				
	});
	}


}
