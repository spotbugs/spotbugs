/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

import de.tobject.findbugs.FindbugsPlugin;

public class OpenXMLResultsAction extends FindBugsAction {

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
        IPath filePath = FindbugsPlugin.getBugCollectionFile(project);
        if (!filePath.toFile().exists()) {
            MessageDialog.openInformation(null, "Open XML results", "No FindBugs analysis results available for project '"
                    + project.getName() + "'!");
            return;
        }
        openEditor(filePath.toFile());
    }

    private IEditorPart openEditor(File file) {
        String editorId = getEditorId(file);
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IFileStore fileStore;
        try {
            fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getCanonicalPath()));
            IEditorInput input = new FileStoreEditorInput(fileStore);
            return page.openEditor(input, editorId);
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not get canonical file path");
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not get canonical file path");
        }
        return null;
    }

    private static String getEditorId(File file) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IEditorRegistry editorRegistry = workbench.getEditorRegistry();
        IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(file.getName(), getContentType(file));
        if (descriptor != null) {
            return descriptor.getId();
        }
        return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
    }

    private static IContentType getContentType(File file) {
        if (file == null) {
            return null;
        }

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return Platform.getContentTypeManager().findContentTypeFor(stream, file.getName());
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "'Open xml' operation failed");
            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                FindbugsPlugin.getDefault().logException(e, "'Open xml' operation failed");
            }
        }
    }

}
