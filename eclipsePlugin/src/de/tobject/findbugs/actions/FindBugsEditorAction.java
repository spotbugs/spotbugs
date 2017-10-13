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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

import de.tobject.findbugs.builder.WorkItem;

/**
 * Run FindBugs on the currently selected element(s) in the package explorer.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 1.1
 * @since 25.09.2003
 */
public class FindBugsEditorAction extends FindBugsAction implements IEditorActionDelegate {

    /** The current selection. */
    private IEditorPart currentEditor;

    @Override
    public final void setActiveEditor(final IAction action, final IEditorPart targetPart) {
        currentEditor = targetPart;
    }

    @Override
    public final void run(final IAction action) {
        if (currentEditor != null) {
            IFile file = ((FileEditorInput) (currentEditor.getEditorInput())).getFile();
            List<WorkItem> list = new ArrayList<>();
            list.add(new WorkItem(file));
            work(currentEditor, file, list);
        }
    }

}
