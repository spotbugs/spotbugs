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

import java.io.IOException;
import java.io.InputStream;

/**
 * Object representing a resource in a code base.
 *
 * @author David Hovemeyer
 */
public interface ICodeBaseEntry {
    /**
     * Get the name of the resource.
     *
     * @return the name of the resource
     */
    public String getResourceName();

    /**
     * Get the number of bytes in the resource. Returns &lt;0 if the number of
     * bytes is not known.
     *
     * @return number of bytes in the resource, or &lt;0 if not known.
     */
    public int getNumBytes();

    /**
     * Open an input stream reading from the resource.
     *
     * @return InputStream reading from the resource.
     * @throws IOException
     *             if an error occurs reading from the resource
     */
    public InputStream openResource() throws IOException;

    /**
     * Get the codebase this codebase entry belongs to.
     *
     * @return the codebase this codebase entry belongs to
     */
    public ICodeBase getCodeBase();

    /**
     * Return the ClassDescriptor of the class resource accessed by this
     * codebase entry. Do not call this method unless
     * ClassDescriptor.isClassResource() returns true. This method may require
     * the class data to be loaded in order to determine the class.
     *
     * @return ClassDescriptor of this entry
     * @throws ResourceNotFoundException InvalidClassFileFormatException
     *             if the codebase entry does not reference a valid classfile
     * @throws IllegalArgumentException
     *             if the codebase entry's filename is definitely not a
     *             classfile
     */
    public ClassDescriptor getClassDescriptor() throws ResourceNotFoundException, InvalidClassFileFormatException;

    /**
     * Override the resource name of this codebase entry.
     *
     * @param resourceName
     *            the new resource name
     */
    public void overrideResourceName(String resourceName);
}
