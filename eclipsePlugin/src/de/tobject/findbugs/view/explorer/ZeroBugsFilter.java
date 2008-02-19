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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.marker.FindBugsMarker;

public class ZeroBugsFilter extends ViewerFilter {

	public ZeroBugsFilter() {
		super();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IResource resource = ResourceUtils.getResource(element);
		if(resource == null) {
			return true;
		}
		if(!resource.isAccessible()) {
			return false;
		}
		try {
			IMarker[] markerArr = resource.findMarkers(FindBugsMarker.NAME, true,
					IResource.DEPTH_INFINITE);
			if (markerArr.length == 0) {
				return false;
			}
			return true;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Core exception on decorateText() for: " + element);
		}
		return true;
	}

}
