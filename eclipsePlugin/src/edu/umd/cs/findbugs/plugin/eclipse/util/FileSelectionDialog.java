/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs.plugin.eclipse.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Peter Hendriks
 */
public class FileSelectionDialog extends ElementTreeSelectionDialog {

	public FileSelectionDialog(Shell parent, String title, final String extension) {
		super(parent, new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
		setTitle(title);
		addFilter(new ViewerFilter() {

			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IContainer) {
					try {
						IResource[] resources = ((IContainer) element)
								.members();
						for (int i = 0; i < resources.length; i++) {
							if (select(viewer, element, resources[i])) {
								return true;
							}
						}
					} catch (CoreException e) {
						FindbugsPlugin.getDefault().logException(e, "Error looking up children.");
					}
				}
				if (element instanceof IFile) {
					IFile file = (IFile) element;
					return file.getName().endsWith(extension);
				}
				return false;
			}

		});
	}
}
