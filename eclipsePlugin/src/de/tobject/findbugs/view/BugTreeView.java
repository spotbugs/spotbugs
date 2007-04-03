package de.tobject.findbugs.view;

import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.HashMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

	public HashMap<String, Tree> projectTrees; //maps project names to corresponding trees

	private HashMap<String, HashMap<String, TreeItem>> patternMap; //maps project names to HashMaps that map strings describing patterns to root TreeItems

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
			if(!(myMarker.getResource().getProject().isOpen()))
			{
				System.out.println("Project not open");
				return;
			}

            FindbugsPlugin.showMarker(myMarker, false, false);
			try{IDE.openEditor(getSite().getPage(), myMarker, false);}
			catch(PartInitException ex){ex.printStackTrace();}
		}

		public void widgetDefaultSelected(SelectionEvent e)
		{
			TreeItem theItem = theTree.getSelection()[0];
			IMarker myMarker = instanceMap.get(theItem);
            if(myMarker == null)
                theItem.setExpanded(!theItem.getExpanded());
            else if(!(myMarker.getResource().getProject().isOpen()))
			{
				System.out.println("Project not open");
				return;
			} else
			{
                FindbugsPlugin.showMarker(myMarker, false, false);

				try{IDE.openEditor(getSite().getPage(), myMarker, false);}
				catch(PartInitException ex){ex.printStackTrace();}
			}
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
		// initialize views with marker data
		IProject[] projectList = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject proj : projectList)
            if(proj.isAccessible() && FindbugsPlugin.isJavaProject(proj)) {
			try{
					for(IMarker marker : proj.findMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE))
						addMarker(proj, marker);
			}
			catch(CoreException e)
			{
				FindbugsPlugin.getDefault().logException(e, "Core exception on tree initialization");
			}
		}
		//add resource change listener to check for project cloure (bug 1674457)
		/*
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IResourceChangeListener listener = new IResourceChangeListener() {
				public void resourceChanged(IResourceChangeEvent event) {
					if(event.getDelta().getKind() == IResourceDelta.OPEN)
						if(event.getResource() instanceof IProject)
						{
							String projectName = event.getResource().getName();
							Tree theTree = projectTrees.get(projectName);
							TabItem theTabItem = projectTabItems.get(projectName);
							if(theTabItem.getControl() == theTree)
							{
								Label theLabel = new Label(theFolder, SWT.VERTICAL);
								theLabel.setText("This project is currently closed. Bug information is unavailable.");
								theTabItem.setControl(theLabel);
							}
							else
								theTabItem.setControl(theTree);
						}
				}
			};
		workspace.addResourceChangeListener(listener);*/
	}

    @Override
    public void dispose() {
        theFolder.dispose();
        super.dispose();
    }

	public void clearTree(IProject currProject)
	{
		this.theProject = currProject;
		Display.getDefault().syncExec(new Runnable(){
			public void run(){
				Tree treeToRemove = projectTrees.get(theProject.getName());
				if(treeToRemove == null) return;
                if (treeToRemove.isDisposed()) return;
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
                    Tree theTree = projectTrees.get(theProject.getName());
                    if (theTree == null || theTree.isDisposed()) {

						TabItem newTab = new TabItem(theFolder, SWT.LEFT);
						theTree = new Tree(theFolder, SWT.LEFT);
                        theTree.addSelectionListener(new BugTreeSelectionListener(theTree));
						newTab.setControl(theTree);
						newTab.setText(theProject.getName());
						projectTrees.put(theProject.getName(), theTree);
						patternMap.put(theProject.getName(), new HashMap<String, TreeItem>());
					}

					HashMap<String, TreeItem> theMap = patternMap.get(theProject.getName());
					BugInstance bug = MarkerUtil.findBugInstanceForMarker(theMarker);
                    if (bug == null) {
                        FindbugsPlugin.getDefault().logWarning("Couldn't find bug for " + theMarker);
                        return;
                    }
					String pattern = bug.getBugPattern().getShortDescription();
					if(!theMap.containsKey(pattern))
					{
						int i;
						for(i=0; i<theTree.getItemCount(); i++)
						{
							if(theTree.getItem(i).getText().compareTo(pattern) > 0)
								break;
						}
						TreeItem newItem = new TreeItem(theTree, SWT.LEFT, i);
						newItem.setText(pattern);
						theMap.put(pattern, newItem);
					}
					TreeItem instanceItem = new TreeItem(theMap.get(pattern), SWT.LEFT);
					instanceMap.put(instanceItem, theMarker);
					instanceItem.setText(theMarker.getAttribute(IMarker.MESSAGE, "Error retrieving message"));}
				catch(Exception e){e.printStackTrace();}
			}
		});
	}
}
