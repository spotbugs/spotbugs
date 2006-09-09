package edu.umd.cs.findbugs.gui2;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
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
	public Component getTreeCellRendererComponent(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) 
	{
		Component toReturn=super.getTreeCellRendererComponent(tree,node,selected,expanded,leaf,row,hasFocus);
		
		if (!(node instanceof BugLeafNode))
			return toReturn;
		else
		{
			BugInstance bug=((BugLeafNode) node).getBug();
			Color c;
			switch (bug.getPriority())
			{
				case Detector.LOW_PRIORITY:
					c=new Color(0.4f, 0.4f, 0.6f);
					break;
				case Detector.NORMAL_PRIORITY:
					c=Color.black;
					break;
				case Detector.HIGH_PRIORITY:
					c=new Color(.85f, 0, 0);
					break;
				case Detector.EXP_PRIORITY: 
				case Detector.IGNORE_PRIORITY:
				default: 
					c=Color.black;
					break;
			}
			if (leaf)
				toReturn.setForeground(c);
			return toReturn;
		}
	}
}	

