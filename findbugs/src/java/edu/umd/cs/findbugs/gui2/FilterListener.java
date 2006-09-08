package edu.umd.cs.findbugs.gui2;

import javax.swing.tree.TreePath;

/**
 * Implemented by BugTreeModel.  Allows the model to clear its
 * cache when the set of filters changes.
 */
public interface FilterListener
{
	public static final int FILTERING=100;
	public static final int UNFILTERING=101;
	public static final int SUPPRESSING=102;
	public static final int UNSUPPRESSING=103;
	
	public void clearCache();
	public void suppressBug(TreePath path);
	public void unsuppressBug(TreePath path);
	
}
