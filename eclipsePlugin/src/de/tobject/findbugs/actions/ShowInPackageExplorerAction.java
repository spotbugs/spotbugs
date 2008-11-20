package de.tobject.findbugs.actions;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ISetSelectionTarget;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;

public class ShowInPackageExplorerAction implements IObjectActionDelegate {

	private BugGroup group;
	private IWorkbenchPartSite site;

	public ShowInPackageExplorerAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		site = targetPart.getSite();
	}

	public void run(IAction action) {
		Object data = group.getData();
        IViewPart part = getView(JavaUI.ID_PACKAGES);
        if(part instanceof ISetSelectionTarget){
            ISetSelectionTarget target = (ISetSelectionTarget) part;
            target.selectReveal(new StructuredSelection(data));
        }
	}

    private IViewPart getView(String id){
        IViewPart part = site.getPage().findView(id);
        if(part == null){
            try {
                part = site.getPage().showView(id);
            } catch (PartInitException e) {
                FindbugsPlugin.getDefault().logException(e, "Can't open view: " + id);
            }
        }
        return part;
    }

	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			group = null;
			action.setEnabled(false);
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		if (ss.size() != 1) {
			group = null;
			action.setEnabled(false);
			return;
		}
		Object firstElement = ss.getFirstElement();
		if (!(firstElement instanceof BugGroup)) {
			group = null;
			action.setEnabled(false);
			return;
		}
		group = (BugGroup) firstElement;
		if (group.getType() == GroupType.Class || group.getType() == GroupType.Package
				|| group.getType() == GroupType.Project) {
			action.setEnabled(true);
		} else {
			group = null;
			action.setEnabled(false);
		}
	}

}
