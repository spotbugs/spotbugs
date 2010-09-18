/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.bugReporter;

import java.util.Set;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PropertyBundle;

/**
 * @author pugh
 */
public class BugReporterPlugin {

    public BugReporterPlugin(Plugin plugin, String filterId, ClassLoader classLoader,
             Class<? extends BugReporterDecorator> filterClass, PropertyBundle properties, boolean enabledByDefault, String description, String details) {
        this.plugin = plugin;
		this.id = filterId;
        int i = filterId.lastIndexOf('.');
        this.shortId = filterId.substring(i+1);
        this.classLoader = classLoader;
		this.filterClass = filterClass;
        this.properties = properties;
        this.description = description;
        this.details = details;
		this.enabledByDefault = enabledByDefault;
    }

    public String getId() {
        return id;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Class<? extends BugReporterDecorator> getBugReporterClass() {
        return filterClass;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public PropertyBundle getProperties() {
        return properties;
    }

    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return getDescription();
    }

	public Plugin getPlugin() {
        return plugin;
    }
    public boolean isNamed(Set<String> names) {
		return names.contains(id) ||  names.contains(shortId);

    }

    final Plugin plugin;
    final String id;
    final String shortId;

    final ClassLoader classLoader;

    final Class<? extends BugReporterDecorator> filterClass;

    final PropertyBundle properties;

    final String description;

    final String details;

    final boolean enabledByDefault;

}
