/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.properties.test;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;

import de.tobject.findbugs.properties.FilterFilesTab;
import de.tobject.findbugs.properties.FindbugsPropertyPage;

/**
 * Test subclass of FilterFilesTab that provides methods for testing purposes.
 * 
 * @author Tomás Pollak
 */
public class FilterFilesTabTestSubclass extends FilterFilesTab {

	public FilterFilesTabTestSubclass(TabFolder tabFolder, FindbugsPropertyPage page,
			int style) {
		super(tabFolder, page, style);
	}

	public void addFileToExcludeBugsFilter(String file) {
		((FilterProviderTestSubclass) getFilterExclBugs()).addFile(file);
	}

	public void addFileToExcludeFilter(String file) {
		((FilterProviderTestSubclass) getFilterExcl()).addFile(file);
	}

	public void addFileToIncludeFilter(String file) {
		((FilterProviderTestSubclass) getFilterIncl()).addFile(file);
	}

	public void removeFilesFromExcludeBugsFilter() {
		((FilterProviderTestSubclass) getFilterExclBugs()).removeAllFiles();
	}

	public void removeFilesFromExcludeFilter() {
		((FilterProviderTestSubclass) getFilterExcl()).removeAllFiles();
	}

	public void removeFilesFromIncludeFilter() {
		((FilterProviderTestSubclass) getFilterIncl()).removeAllFiles();
	}

	@Override
	protected FilterProvider createFilterProvider(ListViewer viewer, FilterKind kind) {
		return new FilterProviderTestSubclass(viewer, kind);
	}

	private class FilterProviderTestSubclass extends FilterProvider {

		private String fileLocation;
		private String parentPath;
		private String fileName;

		protected FilterProviderTestSubclass(ListViewer viewer, FilterKind kind) {
			super(viewer, kind);
		}

		public void addFile(String fileLocation) {
			File file = new File(fileLocation);
			this.fileLocation = fileLocation;
			this.fileName = file.getName();
			this.parentPath = file.getParent();
			addFiles(FilterFilesTabTestSubclass.this.getShell());
		}

		public void removeAllFiles() {
			for (PathElement pathElement : new ArrayList<PathElement>(paths)) {
				remove(pathElement);
			}
		}

		@Override
		protected String[] getFileNames(FileDialog dialog) {
			return new String[] { fileName };
		}

		@Override
		protected String getFilterPath(FileDialog dialog) {
			return parentPath;
		}

		@Override
		protected String openFileDialog(FileDialog dialog) {
			return fileLocation;
		}

	}
}
