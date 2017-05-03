/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.ba.Location;

/**
 * A ResourceCollection defines all of the resources created and used in a
 * particular method. It serves two related purposes:
 * <ol>
 * <li>Define all of the resources which exist in the method, and
 * <li>Record where resources created in the method are created
 * </ol>
 * <p>
 * This distinction is important because some resources which exist in the
 * method aren't created in the method: for example, resources passed in as
 * parameters.
 *
 * @author David Hovemeyer
 */
public class ResourceCollection<Resource> {
    private final List<Resource> resourceList;

    private final Map<Location, Resource> locationToResourceMap;

    /**
     * Constructor. Creates empty collection.
     */
    public ResourceCollection() {
        this.resourceList = new LinkedList<Resource>();
        this.locationToResourceMap = new HashMap<Location, Resource>();
    }

    /**
     * Add a preexisting resource. That is, one that is not created within the
     * analyzed method. Resources passed to the method as parameters fall into
     * this category.
     *
     * @param resource
     *            the preexisting resource
     */
    public void addPreexistingResource(Resource resource) {
        resourceList.add(resource);
    }

    /**
     * Add a resource created within the analyzed method.
     *
     * @param location
     *            the location
     * @param resource
     *            the resource created at that location
     */
    public void addCreatedResource(Location location, Resource resource) {
        resourceList.add(resource);
        locationToResourceMap.put(location, resource);
    }

    /**
     * Return whether or not there are any resources in the collection.
     */
    public boolean isEmpty() {
        return resourceList.isEmpty();
    }

    /**
     * Get an Iterator over all resources in the method. This includes both
     * preexisting and created resources.
     */
    public Iterator<Resource> resourceIterator() {
        return resourceList.iterator();
    }

    /**
     * Get the resource that is created at given location.
     *
     * @param location
     *            the Location
     * @return the Resource created at that location, or null if no resource is
     *         created at the location
     */
    public Resource getCreatedResource(Location location) {
        return locationToResourceMap.get(location);
    }
}

