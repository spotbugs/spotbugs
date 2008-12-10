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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * @author Andrei
 */
public class BugByIdFilter extends ViewerFilter {

	private static String lastUsedFilter;

	static {
		final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
		lastUsedFilter = store.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
		if(lastUsedFilter == null){
			lastUsedFilter = "";
		}
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				lastUsedFilter = store
						.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
			}
		});
	}

	public BugByIdFilter() {
		super();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(lastUsedFilter.length() == 0){
			return true;
		}
		if(element instanceof IMarker){
			IMarker marker = (IMarker) element;
			if(MarkerUtil.isFiltered(marker, lastUsedFilter)){
				return false;
			}
		}
		return true;
	}

}
