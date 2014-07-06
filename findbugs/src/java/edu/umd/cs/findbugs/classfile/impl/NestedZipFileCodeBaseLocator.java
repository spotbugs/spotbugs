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

package edu.umd.cs.findbugs.classfile.impl;

import java.io.IOException;

import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Codebase locator for a zip/jar archive nested inside a parent codebase.
 *
 * @author David Hovemeyer
 */
public class NestedZipFileCodeBaseLocator implements ICodeBaseLocator {
    private final ICodeBase parentCodeBase;

    private final String resourceName;

    public NestedZipFileCodeBaseLocator(ICodeBase parentCodeBase, String resourceName) {
        this.parentCodeBase = parentCodeBase;
        this.resourceName = resourceName;
    }

    /**
     * @return Returns the parentCodeBase.
     */
    public ICodeBase getParentCodeBase() {
        return parentCodeBase;
    }

    /**
     * @return Returns the resourceName.
     */
    public String getResourceName() {
        return resourceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.ICodeBaseLocator#createRelativeCodeBaseLocator
     * (java.lang.String)
     */
    @Override
    public ICodeBaseLocator createRelativeCodeBaseLocator(String relativePath) {
        // The relative path indicates another codebase (archive) in the same
        // parent codebase
        return new NestedZipFileCodeBaseLocator(parentCodeBase, relativePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBaseLocator#openCodeBase()
     */
    @Override
    public ICodeBase openCodeBase() throws ResourceNotFoundException, IOException {
        return ClassFactory.createNestedZipFileCodeBase(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "nested:[" + parentCodeBase.getCodeBaseLocator() + "]" + resourceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        NestedZipFileCodeBaseLocator other = (NestedZipFileCodeBaseLocator) obj;
        return this.parentCodeBase.equals(other.parentCodeBase) && this.resourceName.equals(other.resourceName);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 7919 * parentCodeBase.hashCode() + resourceName.hashCode();
    }
}
