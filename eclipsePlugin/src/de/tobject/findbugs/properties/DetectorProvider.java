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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.widgets.FileDialog;

import de.tobject.findbugs.builder.FindBugsWorker;
import edu.umd.cs.findbugs.config.UserPreferences;

class DetectorProvider extends PathsProvider {

    protected DetectorProvider(CheckboxTableViewer viewer, FindbugsPropertyPage propertyPage) {
        super(viewer, propertyPage);
        setDetectorPlugins(propertyPage.getCurrentUserPreferences());
    }

    List<IPathElement> getDetectorPluginFiles(UserPreferences userPreferences) {
        // TODO project is currently not supported (always null).
        IProject project = propertyPage.getProject();
        final List<IPathElement> newPaths = new ArrayList<IPathElement>();
        Map<String, Boolean> pluginPaths = userPreferences.getCustomPlugins();
        if (pluginPaths != null) {
            Set<Entry<String,Boolean>> entrySet = pluginPaths.entrySet();
            for (Entry<String, Boolean> entry : entrySet) {
                IPath pluginPath = FindBugsWorker.getFilterPath(entry.getKey(), project);
                PathElement element = new PathElement(pluginPath, Status.OK_STATUS);
                element.setEnabled(entry.getValue().booleanValue());
                newPaths.add(element);
            }
        }
        return newPaths;
    }

    @Override
    protected void applyToPreferences() {
        super.applyToPreferences();
        propertyPage.getCurrentUserPreferences().setCustomPlugins(pathsToStrings());
    }

    void setDetectorPlugins(UserPreferences userPreferences) {
        setFilters(getDetectorPluginFiles(userPreferences));
    }

    @Override
    protected IStatus validate() {
        DetectorValidator validator = new DetectorValidator();
        IStatus bad = null;
        for (IPathElement path : paths) {
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
