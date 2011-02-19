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

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.tobject.findbugs.builder.WorkItem;

/**
 * Remove all bug markers for the currently selected editor.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 25.09.2003
 */
public class ClearMarkersEditorAction implements IEditorActionDelegate {

    private IEditorPart currentEditor;

    public final void setActiveEditor(final IAction action, final IEditorPart targetPart) {
        currentEditor = targetPart;
    }

    public final void selectionChanged(final IAction action, final ISelection selection) {
        // noop
    }

    public final void run(final IAction action) {
        if (currentEditor != null) {
            IFile file = ((FileEditorInput) (currentEditor.getEditorInput())).getFile();
            Job job = new ClearMarkersJob(file, Arrays.asList(new WorkItem[] { new WorkItem(file) }));
            job.setUser(true);
            job.setPriority(Job.INTERACTIVE);
            IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) currentEditor.getEditorSite().getService(
                    IWorkbenchSiteProgressService.class);
            service.schedule(job);
        }
    }
}
