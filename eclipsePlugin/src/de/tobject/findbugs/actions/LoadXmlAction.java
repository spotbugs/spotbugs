/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese, 2008 Andrei Loskutov
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

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;

public class LoadXmlAction extends FindBugsAction {

    private static final String DIALOG_SETTINGS_SECTION = "LoadXMLDialogSettings"; //$NON-NLS-1$

    private static final String LOAD_XML_PATH_KEY = "LoadXMLPathSetting"; //$NON-NLS-1$

    @Override
    public void run(final IAction action) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return;
        }
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;

        IProject project = getProject(structuredSelection);
        if (project == null) {
            return;
        }

        // Get the file name from a file dialog
        FileDialog dialog = createFileDialog(project);
        boolean validFileName = false;
        do {
            String fileName = openFileDialog(dialog);
            if (fileName == null) {
                // user cancel
                return;
            }
            validFileName = validateSelectedFileName(fileName);
            if (!validFileName) {
                MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Warning", fileName
                        + " is not a file or is not readable!");
                continue;
            }
            getDialogSettings().put(LOAD_XML_PATH_KEY, fileName);
            work(project, fileName);
        } while (!validFileName);
    }

    protected String openFileDialog(FileDialog dialog) {
        return dialog.open();
    }

    private boolean validateSelectedFileName(String fileName) {
        if (fileName == null) {
            return false;
        }
        File file = new File(fileName);
        return file.isFile() && file.canRead();
    }

    private FileDialog createFileDialog(IProject project) {
        FileDialog fileDialog = new FileDialog(FindbugsPlugin.getShell(), SWT.APPLICATION_MODAL | SWT.OPEN);
        fileDialog.setText("Select bug result xml for project: " + project.getName());
        String initialFileName = getDialogSettings().get(LOAD_XML_PATH_KEY);
        if (initialFileName != null && initialFileName.length() > 0) {
            File initialFile = new File(initialFileName);
            // have to check if exists, otherwise crazy GTK will ignore preset
            // filter
            if (initialFile.exists()) {
                fileDialog.setFileName(initialFile.getName());
            }
            fileDialog.setFilterPath(initialFile.getParent());
        }
        return fileDialog;
    }

    @Override
    protected String getDialogSettingsId() {
        return DIALOG_SETTINGS_SECTION;
    }

    /**
     * Run a FindBugs import on the given project, displaying a progress
     * monitor.
     *
     * @param project
     *            The resource to load XMl to.
     */
    private void work(final IProject project, final String fileName) {
        FindBugsJob runFindBugs = new FindBugsJob("Loading XML data from " + fileName + "...", project) {
            @Override
            protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
                FindBugsWorker worker = new FindBugsWorker(project, monitor);
                worker.loadXml(fileName);
            }
        };
        runFindBugs.setRule(project);
        runFindBugs.scheduleInteractive();
    }

}
