/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import edu.umd.cs.findbugs.ba.Location;

/**
 * A resource creation point. This serves as an embodiment of the resource for
 * use with ResourceValueAnalysis.
 *
 * @author David Hovemeyer
 * @see edu.umd.cs.findbugs.ba.ResourceValueAnalysis
 * @see ResourceTrackingDetector
 */
public class ResourceCreationPoint {
    /**
     * Location in the method where the resource is created.
     */
    private final Location location;

    /**
     * The type of the resource.
     */
    private final String resourceClass;

    /**
     * Constructor.
     *
     * @param location
     *            location where resource is created
     * @param resourceClass
     *            the name of the resource's class
     */
    public ResourceCreationPoint(Location location, String resourceClass) {
        this.location = location;
        this.resourceClass = resourceClass;
    }

    /**
     * Get the location where the resource is created.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the name of the resource's class.
     */
    public String getResourceClass() {
        return resourceClass;
    }
}

