/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Tomas Pollak, Andrei Loskutov
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
package de.tobject.findbugs.actions;

import java.io.File;
import java.io.IOException;

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
import edu.umd.cs.findbugs.BugCollection;

public class SaveXmlAction extends FindBugsAction {

    private static final String DIALOG_SETTINGS_SECTION = "SaveXMLDialogSettings"; //$NON-NLS-1$

    private static final String SAVE_XML_PATH_KEY = "SaveXMLPathSetting"; //$NON-NLS-1$

    /*
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
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
        FileDialog fileDialog = createFileDialog(project);
        boolean validFileName = false;
        do {
            String fileName = openFileDialog(fileDialog);
            if (fileName == null) {
                // User cancelled
                break;
            }
            validFileName = validateSelectedFileName(fileName);
            if (!validFileName) {
                MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Warning", fileName
                        + " is not a file or is not writable!");
                continue;
            }
            if (new File(fileName).exists()) {
                if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Warning", fileName
                        + " already exists. Override?")) {
                    continue;
                }
            }
            getDialogSettings().put(SAVE_XML_PATH_KEY, fileName);
            work(project, fileName);
        } while (!validFileName);
    }

    protected String openFileDialog(FileDialog dialog) {
        return dialog.open();
    }

    private FileDialog createFileDialog(IProject project) {
        FileDialog fileDialog = new FileDialog(FindbugsPlugin.getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
        fileDialog.setText("Select bug result xml for project: " + project.getName());
        String initialFileName = getDialogSettings().get(SAVE_XML_PATH_KEY);
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

    private boolean validateSelectedFileName(String fileName) {
        if (fileName == null) {
            return false;
        }
        File file = new File(fileName);
        return !file.exists() || (file.isFile() && file.canWrite());
    }

    @Override
    protected String getDialogSettingsId() {
        return DIALOG_SETTINGS_SECTION;
    }

    /**
     * Save the XML result of a FindBugs analysis on the given project,
     * displaying a progress monitor.
     *
     * @param project
     *            The selected project.
     * @param fileName
     *            The file name to store the XML to.
     */
    private void work(final IProject project, final String fileName) {
        FindBugsJob runFindBugs = new FindBugsJob("Saving FindBugs XML data to " + fileName + "...", project) {
            @Override
            protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
                BugCollection bugCollection = FindbugsPlugin.getBugCollection(project, monitor);
                try {
                    bugCollection.writeXML(fileName);
                } catch (IOException e) {
                    CoreException ex = new CoreException(FindbugsPlugin.createErrorStatus(
                            "Can't write FindBugs bug collection from project " + project + " to file " + fileName, e));
                    throw ex;
                }
            }
        };
        runFindBugs.setRule(project);
        runFindBugs.scheduleInteractive();
    }
}
