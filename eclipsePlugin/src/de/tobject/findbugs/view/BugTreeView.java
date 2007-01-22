package de.tobject.findbugs.view;

import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.HashMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.ide.IDE;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;

public class BugTreeView extends ViewPart{
	
	private TabFolder theFolder;
	
	public static BugTreeView bugTreeView;
	
	public HashMap<String, Tree> projectTrees;
	
	private HashMap<String, HashMap<String, TreeItem>> patternMap; //maps strings describing patterns to TreeItems
	
	private HashMap<TreeItem, IMarker> instanceMap; //maps TreeItems to the markers they represent
	
	private IProject theProject; //used to communicate with run methods
	
	private IMarker theMarker; ///used to communicate with run methods
	
	private class BugTreeSelectionListener extends SelectionAdapter{
		private Tree theTree;
		public BugTreeSelectionListener(Tree theTree)
		{this.theTree = theTree;}
		
		public void widgetSelected(SelectionEvent e)
		{
			IMarker myMarker = instanceMap.get(theTree.getSelection()[0]);
			if(myMarker == null) return;
			DetailsView.showMarker(myMarker, false);
			UserAnnotationsView.showMarker(myMarker, false);
		}
		
		public void widgetDefaultSelected(SelectionEvent e)
		{
			IMarker myMarker = instanceMap.get(theTree.getSelection()[0]);
			if(myMarker == null) return;
			DetailsView.showMarker(myMarker, false);
			UserAnnotationsView.showMarker(myMarker, false);
			try{IDE.openEditor(getSite().getPage(), myMarker);}
			catch(PartInitException ex){ex.printStackTrace();}
		}
	}
	
	
	public static BugTreeView getBugTreeView()
	{
		return bugTreeView;
	}
	
	public static IMarker getMarkerForTreeItem(TreeItem theItem)
	{
		return bugTreeView.instanceMap.get(theItem);
	}
	
	public void setFocus() {}
	
	public void createPartControl(Composite parent)
	{
		theFolder = new TabFolder(parent, SWT.LEFT);
		projectTrees = new HashMap<String, Tree>();
		patternMap = new HashMap<String, HashMap<String, TreeItem>>();
		instanceMap = new HashMap<TreeItem, IMarker>();
		BugTreeView.bugTreeView = this;
	}
	
	public void clearTree(IProject currProject)
	{
		this.theProject = currProject;
		Display.getDefault().syncExec(new Runnable(){
			public void run(){
				Tree treeToRemove = projectTrees.get(theProject.getName());
				if(treeToRemove == null) return;
				for(TreeItem x : treeToRemove.getItems())
					instanceMap.remove(x);
				patternMap.get(theProject.getName()).clear();
				treeToRemove.removeAll();			
			}
		});
	}
	public void addMarker(IProject currProject, IMarker currMarker)
	{
		this.theProject = currProject;
		this.theMarker = currMarker;
		Display.getDefault().syncExec(new Runnable(){
			public void run(){
				try{
					if(!projectTrees.containsKey(theProject.getName()))
					{
						TabItem newTab = new TabItem(theFolder, SWT.LEFT);
						Tree newTree = new Tree(theFolder, SWT.LEFT);
						newTree.addSelectionListener(new BugTreeSelectionListener(newTree));
						newTab.setControl(newTree);
						newTab.setText(theProject.getName());
						projectTrees.put(theProject.getName(), newTree);
						patternMap.put(theProject.getName(), new HashMap<String, TreeItem>());
					}
					Tree theTree = projectTrees.get(theProject.getName());
					HashMap<String, TreeItem> theMap = patternMap.get(theProject.getName());
					BugInstance bug = MarkerUtil.findBugInstanceForMarker(theMarker);
                    if (bug == null)  {
                        FindbugsPlugin.getDefault().logError("Could not find bug for " + theMarker);
                        return;
                    }
                    
					String pattern = bug.getBugPattern().getType();
                    String bugPatternName = I18N.instance().getShortMessageWithoutCode(pattern);
					if(!theMap.containsKey(bugPatternName))
					{
						TreeItem newItem = new TreeItem(theTree, SWT.LEFT);
						newItem.setText(bugPatternName);
						theMap.put(bugPatternName, newItem);
					}
					TreeItem instanceItem = new TreeItem(theMap.get(bugPatternName), SWT.LEFT);
					instanceMap.put(instanceItem, theMarker);
					instanceItem.setText(bug.getMessageWithoutPrefix());}
				catch(Exception e){e.printStackTrace();}
			}
		});
	}
}
