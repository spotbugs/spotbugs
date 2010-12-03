/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.texteditor.ITextEditor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.EditorUtil;

public class BugActionProvider extends CommonActionProvider {

    private WorkingSetFilterActionGroup workingSetActionGroup;

    private IPropertyChangeListener filterChangeListener;

    boolean hasContributedToViewMenu;

    private MyAction doubleClickAction;

    private ICommonActionExtensionSite site;

    private boolean initDone;

    public BugActionProvider() {
        super();
    }

    static class MyAction extends Action implements ISelectionChangedListener {
        private IMarker marker;

        private IFile file;

        private IJavaElement javaElement;

        @Override
        public void run() {
            if (marker == null && file == null && javaElement == null) {
                return;
            }
            try {
                if (javaElement != null) {
                    IEditorPart editor = JavaUI.openInEditor(javaElement, true, true);

                    // if we have both java element AND line info, go to the
                    // line
                    if (editor instanceof ITextEditor && marker != null) {
                        EditorUtil.goToLine(editor, marker.getAttribute(IMarker.LINE_NUMBER, EditorUtil.DEFAULT_LINE_IN_EDITOR));
                    }
                } else if (marker != null) {
                    IDE.openEditor(FindbugsPlugin.getActiveWorkbenchWindow().getActivePage(), marker, true);
                } else {
                    IDE.openEditor(FindbugsPlugin.getActiveWorkbenchWindow().getActivePage(), file, true);
                }
            } catch (PartInitException e) {
                FindbugsPlugin.getDefault().logException(e, "Cannot open editor for marker: " + marker);
            } catch (JavaModelException e) {
                FindbugsPlugin.getDefault().logException(e, "Cannot open editor for java element: " + javaElement);
            }
        }

        void setSelection(IMarker sel) {
            marker = sel;
            javaElement = MarkerUtil.findJavaElementForMarker(marker);
        }

        public void selectionChanged(SelectionChangedEvent event) {
            resetSelection();
            ISelection selection = event.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection ss = (IStructuredSelection) selection;
                if (ss.size() == 1) {
                    Object firstElement = ss.getFirstElement();
                    if (firstElement instanceof IMarker) {
                        // forward doubleClick to doubleClickAction
                        setSelection((IMarker) firstElement);
                    } else if (firstElement instanceof BugGroup) {
                        BugGroup group = (BugGroup) firstElement;
                        Object data = group.getData();
                        if (data instanceof IJavaElement) {
                            javaElement = (IJavaElement) data;
                        }
                        if (data instanceof IAdaptable) {
                            IAdaptable adaptable = (IAdaptable) data;
                            Object adapter = adaptable.getAdapter(IResource.class);
                            if (adapter instanceof IFile) {
                                file = (IFile) adapter;
                            }
                        }
                    }
                }
            }
        }

        private void resetSelection() {
            marker = null;
            file = null;
            javaElement = null;
        }

    }

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        site = aSite;
        super.init(aSite);
        final StructuredViewer viewer = aSite.getStructuredViewer();
        final BugContentProvider provider = BugContentProvider.getProvider(site.getContentService());

        filterChangeListener = new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (!initDone) {
                    return;
                }
                IWorkingSet oldWorkingSet = provider.getCurrentWorkingSet();
                IWorkingSet oldWorkingSet1 = (IWorkingSet) event.getOldValue();
                IWorkingSet newWorkingSet = (IWorkingSet) event.getNewValue();
                if (newWorkingSet != null && (oldWorkingSet == newWorkingSet || oldWorkingSet1 == newWorkingSet)) {
                    return;
                }
                if (viewer != null) {
                    provider.setCurrentWorkingSet(newWorkingSet);
                    if (newWorkingSet == null) {
                        viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
                    } else if (oldWorkingSet != newWorkingSet) {
                        viewer.setInput(newWorkingSet);
                    }
                }
            }
        };


        workingSetActionGroup = new WorkingSetFilterActionGroup(aSite.getViewSite().getShell(), filterChangeListener);
        if (provider == null)
            throw new NullPointerException("no provider");
        workingSetActionGroup.setWorkingSet(provider.getCurrentWorkingSet());
        doubleClickAction = new MyAction();
        // only if doubleClickAction must know tree selection:
        viewer.addSelectionChangedListener(doubleClickAction);
        initDone = true;
    }

    @Override
    public void dispose() {
        site.getStructuredViewer().removeSelectionChangedListener(doubleClickAction);
        super.dispose();
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);

        if (!hasContributedToViewMenu) {
            IMenuManager menuManager = actionBars.getMenuManager();

            // XXX dirty hack to rename silly "Customize View..." menu
            IContributionItem[] items = menuManager.getItems();
            for (IContributionItem item : items) {
                if (item instanceof ActionContributionItem) {
                    ActionContributionItem item2 = (ActionContributionItem) item;
                    String text = item2.getAction().getText();
                    if ("Customize View...".equals(text) || "&Customize View...".equals(text)) {
                        item2.getAction().setText("Toggle Filters...");
                        break;
                    }
                }
            }
            IContributionItem item = menuManager.find("findBugsEclipsePlugin.toggleGrouping.groupDialog");
            if (item != null) {
                menuManager.remove(item);
                menuManager.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, item);
            }
            IMenuManager mm = menuManager.findMenuUsingPath("bugExplorer.menu.group");
            if (mm != null) {
                menuManager.remove(mm);
                menuManager.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, mm);
            }
            workingSetActionGroup.fillActionBars(actionBars);
            hasContributedToViewMenu = true;
        }
        actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, doubleClickAction);

    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        super.fillContextMenu(menu);

        menu.insertBefore(ICommonMenuConstants.GROUP_PORT, new Separator("fb"));
        menu.insertBefore(ICommonMenuConstants.GROUP_PORT, new Separator("fb.project"));
        menu.insertBefore(ICommonMenuConstants.GROUP_PORT, new Separator("fb.filter"));
    }

    // @Override
    // public void updateActionBars() {
    // IStructuredSelection selection = (IStructuredSelection) getContext()
    // .getSelection();
    //
    // }

}
