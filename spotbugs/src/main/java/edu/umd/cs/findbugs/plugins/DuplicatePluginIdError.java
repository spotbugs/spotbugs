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

package edu.umd.cs.findbugs.plugins;

import java.net.URL;

/**
 * @author pugh
 */
public class DuplicatePluginIdError extends Error {


    final String pluginId;
    final URL loadedFrom;
    final URL previouslyLoadedFrom;

    public String getPluginId() {
        return pluginId;
    }

    public URL getLoadedFrom() {
        return loadedFrom;
    }

    public URL getPreviouslyLoadedFrom() {
        return previouslyLoadedFrom;
    }

    /**
     * @param pluginId
     * @param loadedFrom
     */
    public DuplicatePluginIdError(String pluginId, URL loadedFrom, URL previouslyLoadedFrom) {
        super("Mandatory plugin " + pluginId + " from " + loadedFrom + " already loaded from " + previouslyLoadedFrom);
        this.pluginId = pluginId;
        this.loadedFrom = loadedFrom;
        this.previouslyLoadedFrom = previouslyLoadedFrom;
    }

}
