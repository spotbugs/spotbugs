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

package edu.umd.cs.findbugs;

import java.util.Set;


/**
 * @author pugh
 */
public class ComponentPlugin<T> {

    public ComponentPlugin(Plugin plugin, String id, ClassLoader classLoader,
            Class<? extends T> componentClass,
            PropertyBundle properties, boolean enabledByDefault,
            String description, String details) {
        this.plugin = plugin;
        this.id = id;
        int i = id.lastIndexOf('.');
        this.shortId = id.substring(i + 1);

        this.classLoader = classLoader;
        this.componentClass = componentClass;
        this.properties = properties;
        this.enabledByDefault = enabledByDefault;
        this.description = description;
        this.details = details;

    }
    protected final Plugin plugin;

    public String getId() {
        return id;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
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
        return names.contains(id) || names.contains(shortId);
    }

    protected final String id;
    protected final String shortId;
    protected final ClassLoader classLoader;
    protected final PropertyBundle properties;
    protected final String description;
    protected final String details;
    protected final boolean enabledByDefault;

    public Class<? extends T> getComponentClass() {
        if (!isAvailable()) {
            if (FindBugs.isNoAnalysis()) {
                throw new IllegalStateException("No analysis set; no component class loaded for " + getPlugin());
            }
            throw new IllegalStateException("No component class for " + getPlugin());
        }
        return componentClass;
    }

    public boolean isAvailable() {
        return componentClass != null;
    }

    final Class<? extends T> componentClass;
}
