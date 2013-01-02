/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.view.FindBugsPerspectiveFactory;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

/**
 * Run FindBugs on the currently selected element(s) in the package explorer.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 1.1
 * @since 25.09.2003
 */
public class FindBugsAction implements IObjectActionDelegate {

    /** The current selection. */
    protected ISelection selection;

    /** true if this action is used from editor */
    protected boolean usedInEditor;

    private IWorkbenchPart targetPart;

    private static boolean dialogAlreadyShown;

    public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

    public static boolean isFindBugsPerspectiveActive(IWorkbenchPart part) {
        IPerspectiveDescriptor perspective = getWindow(part).getActivePage().getPerspective();
        return perspective != null && FindBugsPerspectiveFactory.ID.equals(perspective.getId());
    }

    public final void selectionChanged(final IAction action, final ISelection newSelection) {
        if (!usedInEditor) {
            this.selection = newSelection;
        }
    }

    public void run(final IAction action) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription description = workspace.getDescription();
        if (!description.isAutoBuilding() && getClass().equals(FindBugsAction.class)) {
            boolean confirm = MessageDialog.openConfirm(null, "Project -> 'Build Automatically' disabled",
                    "You are going to run FindBugs analysis on a not compiled or partially compiled project.\n\n"
                            + "To get reliable analysis results, you should make sure that project is compiled first.\n\n"
                            + "Continue with FindBugs analysis?");
            if (!confirm) {
                return;
            }
        }

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;

            dialogAlreadyShown = false;

            Map<IProject, List<WorkItem>> projectMap = ResourceUtils.getResourcesPerProject(sSelection);

            for (Map.Entry<IProject, List<WorkItem>> e : projectMap.entrySet()) {
                work(targetPart, e.getKey(), e.getValue());
            }
            targetPart = null;
            selection = null;
        }
    }

    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = FindbugsPlugin.getDefault().getDialogSettings();
        String settingsId = getDialogSettingsId();
        IDialogSettings section = settings.getSection(settingsId);
        if (section == null) {
            section = settings.addNewSection(settingsId);
        }
        return section;
    }

    protected final IProject getProject(IStructuredSelection structuredSelection) {
        Object element = structuredSelection.getFirstElement();
        IResource resource = ResourceUtils.getResource(element);
        if (resource == null) {
            return null;
        }
        IProject project = resource.getProject();
        return project;
    }

    protected String getDialogSettingsId() {
        return "findBugsAction";
    }

    /**
     * Run a FindBugs analysis on the given resource, displaying a progress
     * monitor.
     *
     * @param part
     *
     * @param resources
     *            The resource to run the analysis on.
     */
    protected void work(IWorkbenchPart part, final IResource resource, final List<WorkItem> resources) {
        FindBugsJob runFindBugs = new StartedFromViewJob("Finding bugs in " + resource.getName() + "...", resource, resources, part);
        runFindBugs.scheduleInteractive();
    }

    protected static void refreshViewer(IWorkbenchPart targetPart, final List<WorkItem> resources) {
        if (targetPart == null) {
            return;
        }
        ISelectionProvider selProvider = (ISelectionProvider) targetPart.getAdapter(ISelectionProvider.class);
        if (!(selProvider instanceof TreeViewer)) {
            return;
        }
        final TreeViewer viewer = (TreeViewer) selProvider;
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (viewer.getControl().isDisposed()) {
                    return;
                }
                for (WorkItem workItem : resources) {
                    if (workItem.getMarkerTarget() instanceof IProject) {
                        // this element has to be refreshed manually, because
                        // there is no one real
                        // resource associated with it => no resource change
                        // notification after
                        // creating a marker...
                        viewer.refresh(workItem.getCorespondingJavaElement(), true);
                    }
                }
            }
        });
    }

    protected static void askUserToSwitch(IWorkbenchPart part, int warningsNumber) {
        final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        String message = "FindBugs analysis finished, " + warningsNumber
                + " warnings found.\n\nSwitch to the FindBugs perspective?";

        MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(null, "FindBugs analysis finished",
                message, "Remember the choice and do not ask me in the future", false, store,
                FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH);

        boolean remember = dialog.getToggleState();
        int returnCode = dialog.getReturnCode();

        if (returnCode == IDialogConstants.YES_ID) {
            if (remember) {
                store.setValue(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS, true);
            }
            switchPerspective(part);
        } else if (returnCode == IDialogConstants.NO_ID) {
            if (remember) {
                store.setValue(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS, false);
            }
        }
    }

    protected static IWorkbenchWindow getWindow(IWorkbenchPart part) {
        IWorkbenchWindow window;
        IWorkbenchPartSite currentSite = part != null ? part.getSite() : null;
        if (currentSite != null) {
            window = currentSite.getWorkbenchWindow();
        } else {
            window = FindbugsPlugin.getActiveWorkbenchWindow();
        }
        return window;
    }

    protected static void switchPerspective(IWorkbenchPart part) {
        IWorkbenchWindow window = getWindow(part);
        IWorkbenchPage page = window.getActivePage();
        IAdaptable input;
        if (page != null) {
            input = page.getInput();
        } else {
            input = ResourcesPlugin.getWorkspace().getRoot();
        }
        try {
            PlatformUI.getWorkbench().showPerspective(FindBugsPerspectiveFactory.ID, window, input);
        } catch (WorkbenchException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to open FindBugs Perspective");
        }
    }

    private final static class StartedFromViewJob extends FindBugsJob {
        private final List<WorkItem> resources;

        private final IResource resource;

        private final IWorkbenchPart targetPart;

        private StartedFromViewJob(String name, IResource resource, List<WorkItem> resources, IWorkbenchPart targetPart) {
            super(name, resource);
            this.resources = resources;
            this.resource = resource;
            this.targetPart = targetPart;
        }

        @Override
        protected boolean supportsMulticore(){
            return MutexSchedulingRule.MULTICORE;
        }

        @Override
        protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
            FindBugsWorker worker = new FindBugsWorker(resource, monitor);

            worker.work(resources);

            refreshViewer(targetPart, resources);

            checkPerspective();
        }

        private void checkPerspective() {
            if (isFindBugsPerspectiveActive(targetPart)) {
                return;
            }
            final IMarker[] allMarkers = MarkerUtil.getAllMarkers(resource);
            if (allMarkers.length == 0) {
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    if (isFindBugsPerspectiveActive(targetPart)) {
                        return;
                    }
                    final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
                    final boolean ask = store.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH);
                    if (ask && !dialogAlreadyShown) {
                        dialogAlreadyShown = true;
                        askUserToSwitch(targetPart, allMarkers.length);
                    } else if (store.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS)) {
                        switchPerspective(targetPart);
                    }
                }
            });
        }

    }

}
