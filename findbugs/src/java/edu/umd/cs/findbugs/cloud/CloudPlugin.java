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

package edu.umd.cs.findbugs.cloud;

import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NameLookup;

/**
 * @author pugh
 */
public class CloudPlugin {

    final String findbugsPluginId;

    final String cloudid;

    final ClassLoader classLoader;

    final Class<? extends Cloud> cloudClass;

    final Class<? extends NameLookup> usernameClass;

    final PropertyBundle properties;

    final String description;

    final String details;

    final boolean hidden;

    public CloudPlugin(String findbugsPluginId, String cloudid, ClassLoader classLoader, Class<? extends Cloud> cloudClass,
            Class<? extends NameLookup> usernameClass, boolean hidden, PropertyBundle properties, String description,
            String details) {
        this.findbugsPluginId = findbugsPluginId;
        this.cloudid = cloudid;
        this.classLoader = classLoader;
        this.cloudClass = cloudClass;
        this.usernameClass = usernameClass;
        this.properties = properties;
        this.description = description;
        this.details = details;
        this.hidden = hidden;
    }

    public String getFindbugsPluginId() {
        return findbugsPluginId;
    }

    public String getId() {
        return cloudid;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Class<? extends Cloud> getCloudClass() {
        return cloudClass;
    }

    public Class<? extends NameLookup> getUsernameClass() {
        return usernameClass;
    }

    public boolean isHidden() {
        return hidden;
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

    public boolean isOnline() {
        return OnlineCloud.class.isAssignableFrom(cloudClass);
    }

}
