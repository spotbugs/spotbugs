/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA	 02111-1307	 USA
 */
package edu.umd.cs.findbugs.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * Custom cell renderer for the bug tree.
 * We use this to select the tree icons, and to set the
 * text color based on the bug priority.
 */
public class BugCellRenderer extends DefaultTreeCellRenderer {

	private static final BugCellRenderer theInstance = new BugCellRenderer();

	/**
	 * Get the single instance.
	 * 
	 * @return the instance
	 */
	public static BugCellRenderer instance() {
		return theInstance;
	}

	private static final long serialVersionUID = 1L;
	private ImageIcon bugGroupIcon;
	private ImageIcon packageIcon;
	private ImageIcon bugIcon;
	private ImageIcon classIcon;
	private ImageIcon methodIcon;
	private ImageIcon fieldIcon;
	private ImageIcon sourceFileIcon;
	private Object value;

	private BugCellRenderer() {
		ClassLoader classLoader = this.getClass().getClassLoader();
		bugGroupIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug.png"));
		packageIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/package.png"));
		bugIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug2.png"));
		classIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/class.png"));
		methodIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/method.png"));
		fieldIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/field.png"));
		sourceFileIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/sourcefile.png"));
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
												  boolean expanded, boolean leaf, int row, boolean hasFocus) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object obj = node.getUserObject();

		this.value = obj;

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		// Set the icon, depending on what kind of node it is
		if (obj instanceof BugInstance) {
			setIcon(bugIcon);
		} else if (obj instanceof ClassAnnotation) {
			setIcon(classIcon);
		} else if (obj instanceof MethodAnnotation) {
			setIcon(methodIcon);
		} else if (obj instanceof FieldAnnotation) {
			setIcon(fieldIcon);
		} else if (obj instanceof SourceLineAnnotation) {
			setIcon(sourceFileIcon);
		} else if (obj instanceof BugInstanceGroup) {
			// This is a "group" node
			BugInstanceGroup groupNode = (BugInstanceGroup) obj;
			String groupType = groupNode.getGroupType();
			if (groupType == FindBugsFrame.GROUP_BY_CLASS) {
				setIcon(classIcon);
			} else if (groupType == FindBugsFrame.GROUP_BY_PACKAGE) {
				setIcon(packageIcon);
			} else if (groupType == FindBugsFrame.GROUP_BY_BUG_TYPE) {
				setIcon(bugGroupIcon);
							} else if (groupType == FindBugsFrame.GROUP_BY_BUG_CATEGORY) {
				setIcon(bugGroupIcon);
			}
		} else {
			setIcon(null);
		}

		return this;
	}

	@Override
	public Color getTextNonSelectionColor() {
		return getCellTextColor();
	}

	private Color getCellTextColor() {
		// Based on the priority, color-code the bug instance.
		Color color = Color.BLACK;
		if (value instanceof BugInstance) {
			BugInstance bugInstance = (BugInstance) value;
			switch (bugInstance.getPriority()) {
			case Detector.EXP_PRIORITY:
				color = FindBugsFrame.EXP_PRIORITY_COLOR;
				break;
			case Detector.LOW_PRIORITY:
				color = FindBugsFrame.LOW_PRIORITY_COLOR;
				break;
			case Detector.NORMAL_PRIORITY:
				color = FindBugsFrame.NORMAL_PRIORITY_COLOR;
				break;
			case Detector.HIGH_PRIORITY:
				color = FindBugsFrame.HIGH_PRIORITY_COLOR;
				break;
			}
		}
		return color;
	}
}
