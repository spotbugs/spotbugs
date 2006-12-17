/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Thierry Wyss, Marco Busarello 
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * The <CODE>BugResolutionGenerator</CODE> searchs for bug-resolutions, that
 * can be used to fix the specific bug-type.
 * 
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 */
public class BugResolutionGenerator implements IMarkerResolutionGenerator2 {

    public IMarkerResolution[] getResolutions(IMarker marker) {
        try {
            String type = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
            BugResolutionAssociations resolutions = FindbugsPlugin.getDefault().getBugResolutions();
            return resolutions.getBugResolutions(type);
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker has no FindBugs bug-type.");
            return null;
        }
    }

    public boolean hasResolutions(IMarker marker) {
        try {
            String type = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
            BugResolutionAssociations resolutions = FindbugsPlugin.getDefault().getBugResolutions();
            return resolutions.containsBugResolution(type);
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker has no FindBugs bug-type.");
            return false;
        }
    }

}
