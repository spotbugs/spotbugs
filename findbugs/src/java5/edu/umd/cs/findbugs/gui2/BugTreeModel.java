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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;

/*
 * Our TreeModel.  Once upon a time it was a simple model, that queried data, its BugSet, for what to show under each branch
 * Then it got more and more complicated, no one knows why it still seems to work... or why it doesn't if it in fact doesn't.  
 * 
 * Here's a tip, Dont even attempt to deal with suppressions or filtering and their related tree model events without the API
 * for TreeModelEvents open.  And read it three times first.  Ignore the fact that its inconsistent for sending events about the root, just pick one of the things it says and go with it
 * 
 * Heres the order things MUST be done when dealing with suppressions, filtering, unsuppressions... unfiltering... all that fun stuff
 * 
 * Inserts:
 * Update model
 * Get Path
 * Make Event
 * Send Event
 * ResetData
 * 
 * Removes:
 * Get Path
 * Make Event
 * Update Model
 * Send Event
 * ResetData
 * 
 * Restructure:
 * Update Model
 * Get Path
 * Make Event
 * Send Event
 * resetData? hmmm
 * 
 * These may or may not be the orders of events used in suppressBug, unsuppressBug, branchOperations and so forth
 * if they seem to work anyway, I wouldn't touch them.  
 * 
 * changeSet() is what to do when the data set is completely different (loaded a new collection, reran the analysis what have you)
 * changeSet calls rebuild(), which does a very tricky thing, where it makes a new model, and a new JTree, and swaps them in in place of this one, as well as 
 * turning off user input in hopefully every place it needs to be turned off
 * 
 */

/**
 * The treeModel for our JTree
 */
	public class BugTreeModel implements TreeModel, TableColumnModelListener, FilterListener, TreeExpansionListener
	{	
		private BugAspects root = new BugAspects();
		private SorterTableColumnModel st;
		private BugSet data;
		private ArrayList<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
		private JTree tree;
		static Vector<BugLeafNode> selectedBugLeafNodes = new Vector<BugLeafNode>();

		private static final boolean DEBUG = false;
		
		private volatile Thread rebuildingThread;
		private boolean sortOrderChanged;
		private boolean sortsAddedOrRemoved;
		
		
		public BugTreeModel(JTree tree, SorterTableColumnModel st, BugSet data)
		{
			st.addColumnModelListener(this);
			this.tree = tree;
			this.st = st;
			this.data = data;
			BugSet.setAsRootAndCache(this.data);
			root.setCount(data.size());
			FilterMatcher.addFilterListener(this);
			if (DEBUG) 
				this.addTreeModelListener(new TreeModelListener() {

					public void treeNodesChanged(TreeModelEvent arg0) {
						System.out.println("Tree nodes changed");
						System.out.println("  " + arg0.getTreePath());
						
					}

					public void treeNodesInserted(TreeModelEvent arg0) {
						System.out.println("Tree nodes inserted");
						System.out.println("  " + arg0.getTreePath());
						
					}

					public void treeNodesRemoved(TreeModelEvent arg0) {
						System.out.println("Tree nodes removed");
						System.out.println("  " + arg0.getTreePath());
						
					}

					public void treeStructureChanged(TreeModelEvent arg0) {
						System.out.println("Tree structure changed");
						System.out.println("  " + arg0.getTreePath());
						
					}});
		}
		
		public BugTreeModel(BugTreeModel other)
		{
			this.root = new BugAspects(other.root);
			this.st = other.st;
			this.data = new BugSet(other.data);
			//this.listeners = other.listeners;
			this.tree = other.tree;
		}
		
		public void getOffListenerList()
		{
			FilterMatcher.removeFilterListener(this);
			st.removeColumnModelListener(this);
			tree.removeTreeExpansionListener(this);
		}
		
		public Object getRoot()
		{
			return root;
		}

		public Object getChild(Object o, int index)
		{
			BugAspects a = (BugAspects) o;
			if (st.getOrderBeforeDivider().size()==0 && a.size()==0)//Root without any sortables
				return data.get(index);

			try
			{
				if ((a.size() == 0) || (a.last().key != st.getOrderBeforeDivider().get(st.getOrderBeforeDivider().size() - 1)))
				{
					BugAspects child=a.addToNew(enumsThatExist(a).get(index));
					child.setCount(data.query(child).size());
					return child;
				}	
			else
				return data.query(a).get(index);
			}
			catch (IndexOutOfBoundsException e)
			{
				Debug.println("IndexOutOfBounds caught: I am treemodel #" + this + "I am no longer the current treemodel," +
						" my data is cached and I return bad values for getChild.  Something is wrong with rebuild," +
						" since the tree is asking both of us for children");
				return null;
			}

		}

		public int getChildCount(Object o)
		{
//			long start = bean.getCurrentThreadCpuTime();
//			try
//			{
//			System.out.println("# getChildCount [o = " + o + "]");
		//			System.out.println("getChildCount: " + Thread.currentThread().toString());
			if(!(o instanceof BugAspects))
				return 0;
			
			BugAspects a = (BugAspects) o;
					
					if (st.getOrderBeforeDivider().size()==0 && a.size() == 0)//If its the root and we aren't sorting by anything
						return data.size();
					
					if ((a.size() == 0) || (a.last().key != st.getOrderBeforeDivider().get(st.getOrderBeforeDivider().size() - 1)))
//			{
//			System.out.println("#  before enumsThatExist: " + (bean.getCurrentThreadCpuTime() - start));
						return enumsThatExist(a).size();
//			}
					else
//			{
//			System.out.println("#  before query: " + (bean.getCurrentThreadCpuTime() - start));
						return data.query(a).size();
//			}
//			}
//			finally
//			{
//			System.out.println("#  finished: " + (bean.getCurrentThreadCpuTime() - start));
//			}
		}

		
		/*This contract has been changed to return a HashList of Stringpair, our own data structure in which finding the index of an object in the list is very fast*/
		
		private HashList<StringPair> enumsThatExist(BugAspects a)
		{
		//			long start = bean.getCurrentThreadCpuTime();
//			System.out.println(" ## enumsThatExist [a = " + a + "]");
//			try
//			{					
					//StringPair[] toCheck = null;
					if (st.getOrderBeforeDivider().size()==0)
						return null;					
					
//					if (a.size() == 0) // root
//						toCheck = getValues(st.getOrder().get(0));
//					else if (st.getOrder().indexOf(a.get(a.size() - 1).key) == st.getOrder().size() - 1) // last branch
//						return null;
//					else // somewhere in between
//						toCheck = getValues(st.getOrder().get(st.getOrder().indexOf(a.get(a.size() - 1).key) + 1));
//					BugSet set = data.query(a);
//			System.out.println(" ## after query: " + (bean.getCurrentThreadCpuTime() - start));
//					for (StringPair sp : toCheck)
//						if (set.contains(sp))
//							result.add(sp);
//			System.out.println(" ## after loop (" + toCheck.length + " elements): " + (bean.getCurrentThreadCpuTime() - start));
//					return result;
					
					Sortables key = (a.size() == 0 ?
							st.getOrderBeforeDivider().get(0) :
								st.getOrderBeforeDivider().get(st.getOrderBeforeDivider().indexOf(a.last().key) + 1));
					
					String[] all = key.getAll(data.query(a));
					ArrayList<StringPair> result = new ArrayList<StringPair>();
					for (String i : all)
						result.add(new StringPair(key, i));
//			System.out.println(" ## before sort: " + (bean.getCurrentThreadCpuTime() - start));
//					Collections.sort(result, key);
//			System.out.println(" ## after sort: " + (bean.getCurrentThreadCpuTime() - start));
					return new HashList<StringPair>(result);
//			}
//			finally
//			{
//			System.out.println(" ## finished: " + (bean.getCurrentThreadCpuTime() - start));
//			}
		}
		
		public boolean isLeaf(Object o)
		{
			return (o instanceof BugLeafNode);
		}

		public void valueForPathChanged(TreePath arg0, Object arg1) {}

		public int getIndexOfChild(Object parent, Object child)
		{	
			if (parent == null || child == null || isLeaf(parent))
				return -1;
			
			if (isLeaf(child))
			{
				return data.query((BugAspects) parent).indexOf((BugLeafNode) child);
			}
			else
			{
				HashList<StringPair> stringPairs = enumsThatExist((BugAspects) parent);
				if (stringPairs==null)
				{
					//XXX-Threading difficulties-stringpairs is null somehow
					Debug.println("Stringpairs is null on getIndexOfChild!  Error!");
					assert(false);
					return -1; 
				}

				return stringPairs.indexOf(((BugAspects)child).last());
//				for (int i = 0; i < stringPairs.size(); i++)
//					if (stringPairs.get(i).equals(((BugAspects)child).get(((BugAspects)child).size() -1)))
//						return i;
//					
//					if (stringPairArray[i] == ((BugAspects)child).get(((BugAspects)child).size() - 1))
//						return i;
//				return -1;
			}
		}

		public void addTreeModelListener(TreeModelListener listener)
		{
			listeners.add(listener);
		}
		
		public void removeTreeModelListener(TreeModelListener listener)
		{
			listeners.remove(listener);
		}

		/*private static <T extends Enum> T[]  getValues(Class<T> c) {
			try
			{
				Method getValues = c.getMethod("values");
				return (T[]) getValues.invoke(null);
			}
			catch (SecurityException e)
			{
				assert false;
				return null;
			}
			catch (IllegalAccessException e)
			{
				assert false;
				return null;
			}
			catch (NoSuchMethodException e)
			{
				assert false;
				return null;
			}
			catch (InvocationTargetException e)
			{
				throw new RuntimeException(e.getCause());
			}
		}*/
		private static StringPair[] getValues(Sortables key)
		{
			String[] values= key.getAllSorted();
			StringPair[] result = new StringPair[values.length];
			for (int i = 0; i < values.length; i++)
			{
				result[i] = new StringPair(key, values[i]);
			}
			return result;
			
/*			try
			{
				Method m = BugSet.class.getMethod("getAll" + key, new Class[0]);
				String[] values = (String[]) m.invoke(null, new Object[0]);
				StringPair[] result = new StringPair[values.length];
				for (int i = 0; i < values.length; i++)
				{
					result[i] = new StringPair(key, values[i]);
				}
				return result;
			}
			catch(SecurityException e)
			{
				System.err.println("NoOoOOOooOoOo000!!!1!!!1one!");
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException("getAll" + key + " does not exist");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				System.err.println("Make the method getAll" + key + " public or package or ... something.  ..  Now.");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			return null;	*/
			
		}
		
		public void columnAdded(TableColumnModelEvent e)
		{
			sortsAddedOrRemoved=true;
			//rebuild();	
		}
		
		public void columnRemoved(TableColumnModelEvent e)
		{
			sortsAddedOrRemoved=true;
			//rebuild();
		}
		
		public void columnMoved(final TableColumnModelEvent evt)
		{
			if (evt.getFromIndex() == evt.getToIndex())
				return;
			sortOrderChanged=true;
			//rebuild();
		}
		
		
		void changeSet(BugSet set)
		{
			BugSet.setAsRootAndCache(set);
			data=new BugSet(set);
			root.setCount(data.size());
			rebuild();
		}
		
		
		/**
		 * Swaps in a new BugTreeModel and a new JTree
		 *
		 */
		private void rebuild()
		{
			PreferencesFrame.getInstance().freeze();
			st.freezeOrder();
			MainFrame.getInstance().setRebuilding(true);
			NewFilterFromBug.closeAll();

			//If this thread is not interrupting a previous thread, set the paths to be opened when the new tree is complete
			//If the thread is interrupting another thread, dont do this, because you dont have the tree with the correct paths selected
			
			//As of now, it should be impossible to interrupt a rebuilding thread, in another version this may change, so this if statement check is left in, even though it should always be true.
			if (rebuildingThread==null)
				setOldSelectedBugs();
			
//			if (rebuildingThread != null)
//			{
//				System.out.println(rebuildingThread + " interrupted");
//				System.out.println("Paths to open: " + getOldPaths());
//				try
//				{
//					rebuildingThread.interrupt();  
//				}
//				catch(NullPointerException e)
//				{
//					//Consume- The rebuilding thread was set to null as it finished just before we could interrupt it
//				}
//			}
			Debug.println("Please Wait called right before starting rebuild thread");
			pleaseWait();
			rebuildingThread = new Thread()
			{
				@Override
                public void run()
				{	
					try
					{
						/* Start Time */
						

						Debug.println(Thread.currentThread() + " start");
//						System.out.println(st.getOrder());
						BugTreeModel newModel = new BugTreeModel(BugTreeModel.this);
						Debug.println("The new model is TreeModel # " +newModel);
						newModel.listeners = listeners;
						newModel.resetData();
						newModel.data.sortList();
						
//						if (rebuildingThread != this)
//						{
//							System.out.println(this + " quitting before new JTree()");
//							return;
//						}
						JTree newTree = new JTree(newModel);
						
						//						if (rebuildingThread != this)						
//						{
//							System.out.println(this + " quitting after new JTree()");
//							return;
//						}
						newModel.tree = newTree;
						Debug.println("Making new tree from Rebuild, this happens in swing thread");
						MainFrame.getInstance().newTree(newTree,newModel);
						
						rebuildingThread = null;
//						System.out.println(Thread.currentThread() + " finish");
					}
					catch(NullPointerException e)
					{
						if (rebuildingThread==this)
						{
//							Our design has been changed such that the rebuilding thread is never interrupted, if this happens, its a Major error.
//							However, I dont think it happens anymore
//							System.err.println("We got hosed tommy, we got hosed");
							Debug.println(e);
						}
						else
						{
							//This should also never happen
							Debug.println("Interrupted Thread " + this + " encountered exception, exception was consumed.  However, this thread should never have been interrupted");
						}
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						if (rebuildingThread==this)
						{
//							This no longer happens since the rebuilding thread is never interrupted... I hope.
//							System.err.println("We got hosed timmy, we got hosed");
							Debug.println(e);
						}
						else
						{
							//This should also never happen
							Debug.println("Interrupted Thread " + this + " encountered exception, exception was consumed.  However, this thread should never have been interrupted");
						}
					}
//This horrible code here should no longer be necessary, if the rebuilding thread is never interrupted 
//and we dont mess with the tree while its running, what could possibly go wrong?  
//					catch(Exception e)
//					{
//						if (rebuildingThread==this)
//						{
//							System.err.println("We're toast.");
//							e.printStackTrace();
//						}
//						else
//						{/*burrp*/}
//					}
					finally
					{
						getOffListenerList();
						MainFrame.getInstance().setRebuilding(false);
						PreferencesFrame.getInstance().thaw();
						//st.thawOrder should be the last thing that happens, otherwise a very determined user could slip a new order in before we allow him to rebuild the tree, things get out of sync, nothing bad happens, it just looks wrong until he resorts.
						st.thawOrder();
						Debug.println(Thread.currentThread() + " finally");
					}
				}
			};
			rebuildingThread.start();
		}
		
		public void crawl(final ArrayList<BugAspects> path, final int depth)
		{
			for (int i = 0; i < getChildCount(path.get(path.size() - 1)); i++)
				if (depth > 0)
				{
					ArrayList<BugAspects> newPath = new ArrayList<BugAspects>(path);
					newPath.add((BugAspects) getChild(path.get(path.size() - 1), i));
					crawl(newPath, depth - 1);
				}
				else
				{
					for (TreeModelListener l : listeners)
						l.treeStructureChanged(new TreeModelEvent(this, path.toArray()));
				}
		}
		
		
		
		
		void openPreviouslySelected(List<BugLeafNode> selected)
		{
			BugInstance bug=null;
			TreePath path=null;
			Debug.println("Starting Open Previously Selected");
				for (BugLeafNode b: selected)
				{
					try
					{
						bug=b.getBug();
						path=getPathToBug(bug);
						tree.expandPath(path.getParentPath());
						tree.addSelectionPath(path);
					}
					catch(NullPointerException e)
					{
						//Try to recover!
						if (MainFrame.DEBUG) System.err.println("Failure opening a selected node, node will not be opened in new tree");
//						System.err.println(b);  This will be accurate
//						System.err.println(bug);  This will be accurate
						//System.err.println(path);  
//						e.printStackTrace();					
						continue;
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						//System.err.println("Failure opening a selected node");
						//System.err.println(b);
						//System.err.println(bug);
						//System.err.println(path);
						if (MainFrame.DEBUG) System.err.println("Failure opening a selected node, node will not be opened in new tree");
//						e.printStackTrace();
						continue;
					}
				}
	
		}
		
		/* Recursively traverses the tree, opens all nodes matching any bug 
		 * in the list, then creates the full paths to the bugs that are selected
		 * This keeps whatever bugs were selected selected when sorting
		 *	DEPRECATED--Too Slow, use openPreviouslySelected
		 */
		
		public void crawlToOpen(TreePath path, ArrayList<BugLeafNode> bugLeafNodes, ArrayList<TreePath> treePaths)
		{
			for (int i = 0; i < getChildCount(path.getLastPathComponent()); i++)
			{
				if (!isLeaf(getChild(path.getLastPathComponent(), i)))
					for (BugLeafNode p : bugLeafNodes)
					{
						if (p.matches((BugAspects) getChild(path.getLastPathComponent(), i)))
						{
							tree.expandPath(path);
							crawlToOpen(path.pathByAddingChild(getChild(path.getLastPathComponent(), i)), bugLeafNodes, treePaths);
							break;
						}
					}
				else
				{
					for (BugLeafNode b: bugLeafNodes)
					{
						if (getChild(path.getLastPathComponent(),i).equals(b) )
						{
							tree.expandPath(path);
							treePaths.add(path.pathByAddingChild(getChild(path.getLastPathComponent(), i)));
						}
					}
				}
			}
		}
		
		public void resetData()//FIXME:  Does this need a setAsRootAndCache() on the new BugSet?
		{
			data=new BugSet(data);
		}
		
		public void clearCache()
		{
			resetData();
			BugSet.setAsRootAndCache(data);//FIXME:  Should this be in resetData?  Does this allow our main list to not be the same as the data in our tree?
			root.setCount(data.size());

//			for (TreeModelListener l: listeners)
//				l.treeStructureChanged(new TreeModelEvent(this,new Object[]{root}));
			rebuild();
		}
				
		public void unsuppressBug(TreePath path)
		{
			if (path==null)
				return;
			TreePath pathToFirstDeleted=null;
			Object[] objPath=path.getParentPath().getPath();
			ArrayList<Object> reconstruct=new ArrayList<Object>();
			boolean earlyStop=false;
			for (int x=0; x<objPath.length;x++)
			{
				Object o=objPath[x];
				reconstruct.add(o);
				if (o instanceof BugAspects)
				{
					pathToFirstDeleted=new TreePath(reconstruct.toArray());
					((BugAspects)o).setCount(((BugAspects)o).getCount()+1);

					if (((BugAspects)o).getCount()==2 && reconstruct.size() >1)
					{
						earlyStop=true;
						break;
					}
					
					for (TreeModelListener l: listeners)
					{
						if (pathToFirstDeleted.getParentPath()!=null)
							l.treeNodesChanged(new TreeModelEvent(this, pathToFirstDeleted.getParentPath(),new int[]{getIndexOfChild(pathToFirstDeleted.getParentPath().getLastPathComponent(),pathToFirstDeleted.getLastPathComponent())}, new Object[]{pathToFirstDeleted.getLastPathComponent()}));					
					}
				}
			}
						

			if (path.getParentPath()==null)//They are unsuppressing from the root, but we don't allow root to be suppressed, Dont know what to do here
			{
				throw new RuntimeException();
			}

			if (pathToFirstDeleted==null)
			{
				pathToFirstDeleted=path;
			}
			
			if (earlyStop==false)
			{
				pathToFirstDeleted=pathToFirstDeleted.pathByAddingChild(path.getLastPathComponent());
			}
			
			Object parent=pathToFirstDeleted.getParentPath().getLastPathComponent();
			Object child=pathToFirstDeleted.getLastPathComponent();
			
			TreeModelEvent insertionEvent=new TreeModelEvent(this, pathToFirstDeleted.getParentPath(),new int[]{getIndexOfChild(parent,child)}, new Object[]{child});
			for (TreeModelListener l: listeners)			
			{
				l.treeNodesInserted(insertionEvent);
			}
			if (!isLeaf(child))
			{
				TreeModelEvent structureEvent=new TreeModelEvent(this, pathToFirstDeleted,new int[0], new Object[0]);
				for (TreeModelListener l: listeners)			
				{			
					l.treeStructureChanged(structureEvent);
				}
			}
		}

		public void suppressBug(TreePath path)
		{
			Debug.println(path);
			Object[] objPath=path.getParentPath().getPath();
			ArrayList<Object> reconstruct=new ArrayList<Object>();
			for (int x=0; x< objPath.length;x++)
			{
				Object o=objPath[x];
				((BugAspects)o).setCount(((BugAspects)o).getCount()-1);
			}

			for (int x=0; x< objPath.length;x++)
			{
				Object o=objPath[x];
				reconstruct.add(o);
				if (o instanceof BugAspects)
				{
					if (((BugAspects)o).getCount()==0)
					{
						path=new TreePath(reconstruct.toArray());
						break;
					}
				}
			}
			
			TreeModelEvent event;
		
			if (path.getParentPath()==null)//They are suppressing the last bug in the tree
			{
				event=new TreeModelEvent(this,path,new int[]{0},new Object[]{this.getChild(root,0)});
				root.setCount(0);
			}
			else
			{
				Object parent = path.getParentPath().getLastPathComponent();
				Object child = path.getLastPathComponent();
				int indexOfChild=getIndexOfChild(parent,child);
				if (indexOfChild!=-1)
				{
					event=new TreeModelEvent(this, path.getParentPath(),new int[]{indexOfChild}, new Object[]{child});
					resetData();
				}
				else//They are suppressing something that has already been filtered out by setting a designation of a bug to a type that has been filtered out.
				{
					resetData();
					for (TreeModelListener l: listeners)
					{
						l.treeStructureChanged(new TreeModelEvent(this, path.getParentPath()));
					}
					return;
				}
			}
			
			for (TreeModelListener l: listeners)			
			{
				l.treeNodesRemoved(event);
			}
		}

		void treeNodeChanged(TreePath path)
		{
			Debug.println("Tree Node Changed: " + path);
			if (path.getParentPath()==null)
			{
				TreeModelEvent event=new TreeModelEvent(this,path,null,null);
				for (TreeModelListener l:listeners)
				{
					l.treeNodesChanged(event);
				}
				return;
			}
			
			TreeModelEvent event=new TreeModelEvent(this,path.getParentPath(),new int[]{getIndexOfChild(path.getParentPath().getLastPathComponent(),path.getLastPathComponent())},new Object[] {path.getLastPathComponent()});
			for (TreeModelListener l: listeners)
			{
				l.treeNodesChanged(event);
			}
		}
		
		public TreePath getPathToBug(BugInstance b)
		{
			//ArrayList<Sortables> order=MainFrame.getInstance().getSorter().getOrder();
			List<Sortables> order=st.getOrderBeforeDivider();
			//Create an array of BugAspects of lengths from one to the full BugAspect list of the bugInstance	
			BugAspects[] toBug=new BugAspects[order.size()];
			for (int i=0; i < order.size(); i++)
				toBug[i]=new BugAspects();
				
			for (int x=0; x< order.size();x++)
			{
				for (int y=0; y<=x;y++)
				{
					Sortables s = order.get(y);
					toBug[x].add(new StringPair(s,s.getFrom(b)));
				}
			}
			//Add this array as elements of the path
			TreePath pathToBug=new TreePath(root);
			for (int x=0;x<order.size();x++)
			{
				int index=getIndexOfChild(pathToBug.getLastPathComponent(),toBug[x]);

				if (index==-1)
				{
					if (MainFrame.DEBUG) System.err.println("Node does not exist in the tree");//For example, not a bug bugs are filtered, they set a bug to be not a bug it filters out
					return null;
				}
				
				pathToBug=pathToBug.pathByAddingChild(getChild(pathToBug.getLastPathComponent(),index));
			}
			//Using a hashlist to store bugs in BugSet will make getIndexOfChild Waaaaaay faster, thus making this O(1) (avg case)
			int index=getIndexOfChild(pathToBug.getLastPathComponent(),new BugLeafNode(b));
			if(index == -1)
				return null;
			pathToBug=pathToBug.pathByAddingChild(getChild(pathToBug.getLastPathComponent(),index));
			return pathToBug;

		}
		
		public TreePath getPathToNewlyUnsuppressedBug(BugInstance b)
		{
			resetData();
			return getPathToBug(b);
		}
		
		@Override
        protected void finalize() throws Throwable
		{
			super.finalize();
			
			//this will inform us when the garbage collector finds our old bug tree models and deletes them, thus preventing obnoxiously hard to find bugs from not remembering to remove the model from our listeners
			Debug.println("The BugTreeModel has been DELETED!  This means there are no more references to it, and its finally off all of the stupid listener lists");
		}
		
		public void columnMarginChanged(ChangeEvent arg0) {}
		public void columnSelectionChanged(ListSelectionEvent arg0) {}

		public void treeExpanded(TreeExpansionEvent event) {
		}
		
		public void treeCollapsed(TreeExpansionEvent event) {
		}
		
		private void setOldSelectedBugs()
		{
			selectedBugLeafNodes.clear();
			if (tree.getSelectionPaths() != null) // Who the cussword wrote this API anyway?
				for (TreePath path : tree.getSelectionPaths())
					if (isLeaf(path.getLastPathComponent()))
						selectedBugLeafNodes.add((BugLeafNode) path.getLastPathComponent());
		}
		
		Vector<BugLeafNode> getOldSelectedBugs()
		{
			return selectedBugLeafNodes;
		}
		
		public static class PleaseWaitTreeModel implements TreeModel
		{
			private String root = "Please wait...";
			public PleaseWaitTreeModel() {}
			public PleaseWaitTreeModel(String message) {if (message!=null) root=message;}
			
			public void addTreeModelListener(TreeModelListener l) {}
			public Object getChild(Object parent, int index) {return null;}
			public int getChildCount(Object parent) {return 0;}
			public int getIndexOfChild(Object parent, Object child) {return -1;}
			public Object getRoot() {return root;}
			public boolean isLeaf(Object node) {return true;}
			public void removeTreeModelListener(TreeModelListener l) {}
			public void valueForPathChanged(TreePath path, Object newValue) {}
		}
		
		void checkSorter()
		{
			if (sortOrderChanged==true || sortsAddedOrRemoved==true)
			{
				sortOrderChanged=false;
				sortsAddedOrRemoved=false;
				rebuild();
			}
//			This old version isn't wrong... it just worries me, as it looks like we could rebuild twice, although
//			we never do.  Above version should be safer.
//			if (sortOrderChanged==true)
//			{
//				sortOrderChanged=false;
//				rebuild();
//			}
//			if (sortsAddedOrRemoved==true)
//			{
//				sortsAddedOrRemoved=false;
//				rebuild();
//			}
		}		
		
		static void pleaseWait()
		{
			pleaseWait(null);
		}
		static void pleaseWait(final String message)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					MainFrame.getInstance().pleaseWait = true;
					Debug.println("Please Wait! " + (message==null?"":message));
					MainFrame.getInstance().getTree().setModel(new PleaseWaitTreeModel(message));
					MainFrame.getInstance().pleaseWait = false;
					//MainFrame.getInstance().setSorting(false);
					Debug.println("Please Stop Waiting");
				}
			});	
		}
		
		public TreeModelEvent restructureBranch(ArrayList<String> stringsToBranch, boolean removing) throws BranchOperationException
		{
			if (removing)
				return branchOperations(stringsToBranch, TreeModification.REMOVERESTRUCTURE);
			else
				return branchOperations(stringsToBranch, TreeModification.INSERTRESTRUCTURE);
		}
		
		public TreeModelEvent insertBranch(ArrayList<String> stringsToBranch) throws BranchOperationException
		{
			return branchOperations(stringsToBranch, TreeModification.INSERT);
		}
		
		public TreeModelEvent removeBranch(ArrayList<String> stringsToBranch) throws BranchOperationException
		{
			return branchOperations(stringsToBranch, TreeModification.REMOVE);
		}
		
		public void sortBranch(TreePath pathToBranch)
		{
			BugSet bs=data.query((BugAspects)pathToBranch.getLastPathComponent());
			bs.sortList();
			Debug.println("Data in sorted branch: " + pathToBranch.getLastPathComponent());
			for (BugLeafNode b: bs)
			{
				Debug.println(b);
			}
		
			Object[] children=new Object[getChildCount(pathToBranch.getLastPathComponent())];
			int[] childIndices=new int[children.length];
			for (int x=0; x<children.length; x++)
			{
				children[x]=getChild(pathToBranch.getLastPathComponent(),x);
				childIndices[x]=x;
			}
			for (TreeModelListener l: listeners)
			{
				TreeModelEvent event=new TreeModelEvent(this,pathToBranch,childIndices,children);
				l.treeNodesChanged(event);
			}

		}
		
		@SuppressWarnings("serial")
		static class BranchOperationException extends Exception
		{
			public BranchOperationException(String s)
			{
				super(s);
			}
		}
		enum TreeModification {REMOVE, INSERT, REMOVERESTRUCTURE, INSERTRESTRUCTURE};
		private TreeModelEvent branchOperations(ArrayList<String> stringsToBranch, TreeModification whatToDo) throws BranchOperationException
		{
			TreeModelEvent event=null;

			if (whatToDo==TreeModification.REMOVE)
				Debug.println("Removing a branch......");
			else if (whatToDo==TreeModification.INSERT)
				Debug.println("Inserting a branch......");
			else if (whatToDo==TreeModification.REMOVERESTRUCTURE)
				Debug.println("Restructuring from branch to remove......");
			else if (whatToDo==TreeModification.INSERTRESTRUCTURE)
				Debug.println("Restructuring from branch to insert......");
			Debug.println(stringsToBranch);
			
			if (whatToDo==TreeModification.INSERT || whatToDo==TreeModification.INSERTRESTRUCTURE)
			{
				resetData();
			}
			//ArrayList<Sortables> order=MainFrame.getInstance().getSorter().getOrder();
			List<Sortables> order=st.getOrderBeforeDivider();
			//Create an array of BugAspects of lengths from one to the full BugAspect list of the bugInstance	
			BugAspects[] toBug=new BugAspects[stringsToBranch.size()];
			for (int x=0; x < stringsToBranch.size(); x++) {
				toBug[x]=new BugAspects();
	
				for (int y=0; y<=x;y++)
				{
					Sortables s = order.get(y);
					toBug[x].add(new StringPair(s,stringsToBranch.get(y)));
				}
			}
			
			//Add this array as elements of the path
			TreePath pathToBranch=new TreePath(root);
			for (int x=0;x<stringsToBranch.size();x++)
			{
				BugAspects child=toBug[x];
				BugAspects parent=(BugAspects) pathToBranch.getLastPathComponent();
				if (getIndexOfChild(parent,child)!=-1)
				{
					pathToBranch=pathToBranch.pathByAddingChild(child);
				}
				else
				{
					Debug.println(parent + " does not contain " + child);
					throw new BranchOperationException("Branch has been filtered out by another filter.");
//					break;
				}
			}
			if (pathToBranch.getParentPath()!=null)
				while (getChildCount(pathToBranch.getParentPath().getLastPathComponent())==1)
				{
					if (pathToBranch.getParentPath().getLastPathComponent().equals(root))
						break;
					pathToBranch=pathToBranch.getParentPath();
				}
			Debug.println(pathToBranch);

			
			if (whatToDo==TreeModification.INSERT)
			{
				event=new TreeModelEvent(this,pathToBranch.getParentPath(),new int[]{getIndexOfChild(pathToBranch.getParentPath().getLastPathComponent(),pathToBranch.getLastPathComponent())}, new Object[]{pathToBranch.getLastPathComponent()});				
			}
			else if (whatToDo==TreeModification.INSERTRESTRUCTURE)
			{
				event=new TreeModelEvent(this,pathToBranch);
			}
			
			if (whatToDo==TreeModification.REMOVE)
			{
				event=new TreeModelEvent(this,pathToBranch.getParentPath(),new int[]{getIndexOfChild(pathToBranch.getParentPath().getLastPathComponent(),pathToBranch.getLastPathComponent())}, new Object[]{pathToBranch.getLastPathComponent()});
			
			}
			else if (whatToDo==TreeModification.REMOVERESTRUCTURE)
			{
				event=new TreeModelEvent(this,pathToBranch);
			}

			if (whatToDo==TreeModification.REMOVE || whatToDo==TreeModification.REMOVERESTRUCTURE)
				resetData();
			
			return event;
		}		
		
		void sendEvent(TreeModelEvent event, TreeModification whatToDo)
		{
			Debug.println("Sending An Event!");
			if (event==null)
			{
				throw new IllegalStateException("Dont throw null events.");
			}
			resetData();
			for (TreeModelListener l: listeners)
			{
				if (whatToDo==TreeModification.REMOVE)
					l.treeNodesRemoved(event);
				else if (whatToDo==TreeModification.INSERT)
				{
					l.treeNodesInserted(event);
					l.treeStructureChanged(new TreeModelEvent(this,new TreePath(event.getPath()).pathByAddingChild(event.getChildren()[0])));
				}
				else if (whatToDo==TreeModification.INSERTRESTRUCTURE || whatToDo==TreeModification.REMOVERESTRUCTURE)
				{
					l.treeStructureChanged(event);
				}
			}
			
			root.setCount(data.size());
			TreePath changedPath=new TreePath(root);
			treeNodeChanged(changedPath);
			changedPath=new TreePath(event.getPath());
			while (changedPath.getParentPath()!=null)
			{
				treeNodeChanged(changedPath);
				changedPath=changedPath.getParentPath();
			}
		}

	}