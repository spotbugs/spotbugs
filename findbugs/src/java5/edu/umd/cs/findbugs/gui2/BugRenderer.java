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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;

@SuppressWarnings("serial")
/**
 *  Sets colors for JTree nodes
 * 	@author Dan 
 */  
public class BugRenderer extends DefaultTreeCellRenderer
{
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) 
	{
		Component toReturn=super.getTreeCellRendererComponent(tree,node,selected,expanded,leaf,row,hasFocus);

		if (!(node instanceof BugLeafNode))
			return toReturn;
		else
		{
			BugInstance bug=((BugLeafNode) node).getBug();
			final Color c;
			switch (bug.getPriority())
			{
				case Detector.LOW_PRIORITY:
					c=new Color(0.4f, 0.4f, 0.6f);
					break;
				case Detector.NORMAL_PRIORITY:
					if (bug.isDead())
						c = new Color(0.2f, 0.2f, 0.2f);
					else 
						c= new Color(255, 204, 0);
					break;
				case Detector.HIGH_PRIORITY:
					if (bug.isDead())
						c=new Color(.65f, 0.2f, 0.2f);
					else
						c=new Color(.85f, 0, 0);
					break;
				case Detector.EXP_PRIORITY: 
				case Detector.IGNORE_PRIORITY:
				default: 
					c=Color.blue;
					break;
			}
			if (leaf) {
                Icon icon = new Icon() {
                    public void paintIcon(Component comp, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(c);
                        g2.fillOval(2, 2, 12, 12);
                        g2.setColor(Color.BLACK);
                        g2.drawOval(2, 2, 12, 12);
                    }

                    public int getIconWidth() {
                        return 16;
                    }

                    public int getIconHeight() {
                        return 16;
                    }
                };
                ((BugRenderer)toReturn).setLeafIcon(icon);
            }
			return toReturn;
		}
	}
}	

