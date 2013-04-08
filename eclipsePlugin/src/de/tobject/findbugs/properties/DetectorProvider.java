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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.widgets.FileDialog;

import de.tobject.findbugs.DetectorsExtensionHelper;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.properties.DetectorValidator.ValidationStatus;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.config.UserPreferences;

public class DetectorProvider extends PathsProvider {

    protected DetectorProvider(CheckboxTableViewer viewer, FindbugsPropertyPage propertyPage) {
        super(viewer, propertyPage);
        setDetectorPlugins(propertyPage.getCurrentUserPreferences());
    }

    /**
     * The complexity of the code below is partly caused by the fact that we
     * might have multiple ways to install and/or enable custom plugins. There
     * are plugins discovered by FB itself, plugins contributed to Eclipse and
     * plugins added by user manually via properties. Plugins can be disabled
     * via code or properties. The code below is still work in progress, see
     * also {@link FindbugsPlugin#applyCustomDetectors(boolean)}.
     *
     * @return a list with all known plugin paths known by FindBugs (they must
     *         neither be valid nor exists).
     */
    public static List<IPathElement> getPluginElements(UserPreferences userPreferences) {
        DetectorValidator validator = new DetectorValidator();
        final List<IPathElement> newPaths = new ArrayList<IPathElement>();
        Map<String, Boolean> pluginPaths = userPreferences.getCustomPlugins();

        Set<String> disabledSystemPlugins = new HashSet<String>();
        Set<URI> customPlugins = new HashSet<URI>();
        Set<Entry<String,Boolean>> entrySet = pluginPaths.entrySet();
        for (Entry<String, Boolean> entry : entrySet) {
            String idOrPath = entry.getKey();
            if(new Path(idOrPath).segmentCount() == 1) {
                PathElement element = new PathElement(new Path(idOrPath), Status.OK_STATUS);
                element.setSystem(true);
                if (!entry.getValue().booleanValue()) {
                    element.setEnabled(false);
                    // this is not a path => this is a disabled plugin id
                    disabledSystemPlugins.add(idOrPath);
                    newPaths.add(element);
                } else {
                    element.setEnabled(true);
                }
                continue;
            }

            // project is not supported (propertyPage.getProject() == null for workspace prefs).
            IPath pluginPath = FindBugsWorker.getFilterPath(idOrPath, null);
            URI uri = pluginPath.toFile().toURI();
            customPlugins.add(uri);
            ValidationStatus status = validator.validate(pluginPath.toOSString());
            PathElement element = new PathElement(pluginPath, status);
            Plugin plugin = Plugin.getByPluginId(status.getSummary().id);
            if(plugin != null && !uri.equals(plugin.getPluginLoader().getURI())) {
                // disable contribution if the plugin is already there
                // but loaded from different location
                element.setEnabled(false);
            } else {
                element.setEnabled(entry.getValue().booleanValue());
            }
            newPaths.add(element);
        }

        Map<URI, Plugin> allPlugins = Plugin.getAllPluginsMap();

        // List of plugins contributed by Eclipse
        SortedMap<String, String> contributedDetectors = DetectorsExtensionHelper.getContributedDetectors();
        for (Entry<String, String> entry : contributedDetectors.entrySet()) {
            String pluginId = entry.getKey();
            URI uri = new Path(entry.getValue()).toFile().toURI();
            Plugin plugin = allPlugins.get(uri);
            if(plugin != null && !isEclipsePluginDisabled(pluginId, allPlugins)) {
                PluginElement element = new PluginElement(plugin, true);
                newPaths.add(0, element);
                customPlugins.add(uri);
            }
        }

        // Remaining plugins contributed by FB itself
        for (Plugin plugin : allPlugins.values()) {
            PluginElement element = new PluginElement(plugin, false);
            if(!customPlugins.contains(plugin.getPluginLoader().getURI())) {
                newPaths.add(0, element);
                if(disabledSystemPlugins.contains(plugin.getPluginId())) {
                    element.setEnabled(false);
                }
            }
        }
        return newPaths;
    }

    /**
     * Eclipse plugin can be disabled ONLY by user, so it must NOT be in the
     * list of loaded plugins
     */
    static boolean isEclipsePluginDisabled(String pluginId, Map<URI, Plugin> allPlugins) {
        for (Plugin plugin : allPlugins.values()) {
            if(pluginId.equals(plugin.getPluginId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void applyToPreferences() {
        super.applyToPreferences();
        propertyPage.getCurrentUserPreferences().setCustomPlugins(pathsToStrings());
    }

    void setDetectorPlugins(UserPreferences userPreferences) {
        setFilters(getPluginElements(userPreferences));
    }

    @Override
    protected IStatus validate() {
        DetectorValidator validator = new DetectorValidator();
        IStatus bad = null;
        for (IPathElement path : paths) {
            if(path.isSystem()) {
                continue;
            }
            String pathStr = FindBugsWorker.getFilterPath(path.getPath(), null).toOSString();
            ValidationStatus status = validator.validate(pathStr);
            path.setStatus(status);
            if (!status.isOK() && path.isEnabled()) {
                bad = status;
                path.setEnabled(false);
                break;
            }
        }
        return bad;
    }

    @Override
    protected void configureDialog(FileDialog dialog) {
        dialog.setFilterExtensions(new String[] { "*.jar" });
        dialog.setText("Select FindBugs plugins (file must have '.jar' extension)");
    }
}
