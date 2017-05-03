/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile;

import java.util.Iterator;
import java.util.Map;

/**
 * A classpath from which resources (classes and other files) may be loaded.
 * Essentially, this is just a list of codebases.
 *
 * @author David Hovemeyer
 */
public interface IClassPath {
    /**
     * Add a codebase. The object will be interrogated to determine whether it
     * is an application codebase or an auxiliary codebase. Application
     * codebases must be scannable.
     *
     * @param codeBase
     *            the codebase to add
     */
    public void addCodeBase(ICodeBase codeBase);

    /**
     * Return an iterator over the application codebases.
     *
     * @return iterator over the application codebases
     */
    public Iterator<? extends ICodeBase> appCodeBaseIterator();

    /**
     * Return an iterator over the auxiliary codebases.
     *
     * @return iterator over the auxiliary codebases
     */
    public Iterator<? extends ICodeBase> auxCodeBaseIterator();

    /**
     * Lookup a resource by name.
     *
     * @param resourceName
     *            name of the resource to look up
     * @return ICodeBaseEntry representing the resource
     * @throws ResourceNotFoundException
     *             if the resource is not found
     */
    public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException;

    /**
     * Add a resource name to codebase entry mapping. Once this is done, future
     * lookups of this resource will automatically resolve to the given codebase
     * entry.
     *
     * @param resourceName
     *            the resource name to map
     * @param codeBaseEntry
     *            the codebase entry to use for this resource
     */
    public void mapResourceNameToCodeBaseEntry(String resourceName, ICodeBaseEntry codeBaseEntry);

    /**
     * Close all of the code bases that are part of this class path. This should
     * be done once the client is finished with the classpath.
     */
    public void close();

    /**
     * Returns all of the application code base entries that are part of this class path.
     *
     * @return map where the key is slashed (VM) class name with ".class" suffix
     */
    public Map<String, ICodeBaseEntry> getApplicationCodebaseEntries();

}
