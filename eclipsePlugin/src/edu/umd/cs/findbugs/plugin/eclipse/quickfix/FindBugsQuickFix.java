package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

public class FindBugsQuickFix implements IMarkerResolutionGenerator {
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IMarkerResolution[] res = null;
		try {
			String type = (String)marker.getAttribute(FindBugsMarker.BUG_TYPE);
			QuickFixAssociations fixer = FindbugsPlugin.getDefault().getQuickFixes();
			
			res = fixer.createFixers(type);
		} catch (CoreException e) {
			e.printStackTrace();
			res = new IMarkerResolution[] {};
		}
		return res;
	}


}
