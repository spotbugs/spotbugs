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

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * The <CODE>BugResolutionGenerator</CODE> searchs for bug-resolutions, that can
 * be used to fix the specific bug-type.
 *
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 */
public class BugResolutionGenerator implements IMarkerResolutionGenerator2 {

    private BugResolutionAssociations bugResolutions;

    private boolean bugResolutionsLoaded;

    public BugResolutionAssociations getBugResolutions() {
        if (!bugResolutionsLoaded) {
            bugResolutionsLoaded = true;
            try {
                bugResolutions = loadBugResolutions();
            } catch (Exception e) {
                FindbugsPlugin.getDefault().logException(e, "Could not read load bug resolutions");
            }
        }
        return bugResolutions;
    }

    private static BugResolutionAssociations loadBugResolutions() {
        Map<String, List<QuickFixContribution>> quickFixes = QuickFixesExtensionHelper.getContributedQuickFixes();
        return new BugResolutionAssociations(quickFixes);
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        String type = MarkerUtil.getBugPatternString(marker);
        if(type == null){
            return null;
        }
        BugResolutionAssociations resolutions = getBugResolutions();
        if (resolutions == null) {
            return new IMarkerResolution[0];
        }
        return resolutions.createBugResolutions(type, marker);
    }

    @Override
    public boolean hasResolutions(IMarker marker) {
        String type = MarkerUtil.getBugPatternString(marker);
        if(type == null){
            return false;
        }
        BugResolutionAssociations resolutions = getBugResolutions();
        if (resolutions == null) {
            return false;
        }
        return resolutions.containsBugResolution(type);
    }

}
