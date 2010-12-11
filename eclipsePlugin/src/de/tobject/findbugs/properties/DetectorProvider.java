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
package de.tobject.findbugs.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.widgets.FileDialog;

import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.preferences.PrefsUtil;

class DetectorProvider extends PathsProvider {

    protected DetectorProvider(CheckboxTableViewer viewer, FindbugsPropertyPage propertyPage) {
        super(viewer, propertyPage);
        setDetectorPlugins(propertyPage.getPreferenceStore());
    }

    List<PathElement> getDetectorPluginFiles(IPreferenceStore prefs) {
        // TODO project is currently not supported (always null).
        IProject project = propertyPage.getProject();
        final List<PathElement> newPaths = new ArrayList<PathElement>();
        Collection<String> filterPaths = PrefsUtil.readDetectorPaths(prefs);
        if (filterPaths != null) {
            for (String path : filterPaths) {
                IPath filterPath = FindBugsWorker.getFilterPath(path, project);
                // if(filterPath.toFile().exists()) {
                newPaths.add(new PathElement(filterPath, Status.OK_STATUS));
                // }
            }
        }
        return newPaths;
    }

    @Override
    protected void applyToPreferences() {
        super.applyToPreferences();
        PrefsUtil.writeDetectorPaths(propertyPage.getPreferenceStore(), pathsToStrings());
    }

    void setDetectorPlugins(IPreferenceStore prefs) {
        setFilters(getDetectorPluginFiles(prefs));
    }

    @Override
    protected IStatus validate() {
        DetectorValidator validator = new DetectorValidator();
        IStatus bad = null;
        for (PathElement path : paths) {
            IStatus status = validator.validate(path.getPath());
            path.setStatus(status);
            if (!status.isOK()) {
                bad = status;
                break;
            }
        }
        return bad;
    }

    @Override
    protected void configureDialog(FileDialog dialog) {
        dialog.setFilterExtensions(new String[] { "*.jar" });
        dialog.setText("Select jar file(s) containing custom detectors");
    }
}
