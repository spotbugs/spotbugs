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
import java.io.InputStream;
import java.util.zip.ZipEntry;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * Implementation of ICodeBaseEntry for resources in zipfile codebases.
 *
 * @author David Hovemeyer
 */
public class ZipFileCodeBaseEntry extends AbstractScannableCodeBaseEntry {
    private final ZipFileCodeBase codeBase;

    private final ZipEntry zipEntry;

    public ZipFileCodeBaseEntry(ZipFileCodeBase codeBase, ZipEntry zipEntry) {
        this.codeBase = codeBase;
        this.zipEntry = zipEntry;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getNumBytes()
     */
    @Override
    public int getNumBytes() {
        return (int) zipEntry.getSize();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#openResource()
     */
    @Override
    public InputStream openResource() throws IOException {
        return codeBase.zipFile.getInputStream(zipEntry);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.impl.AbstractScannableCodeBaseEntry#getCodeBase
     * ()
     */
    @Override
    public AbstractScannableCodeBase getCodeBase() {
        return codeBase;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.impl.AbstractScannableCodeBaseEntry#
     * getRealResourceName()
     */
    @Override
    public String getRealResourceName() {
        return zipEntry.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getClassDescriptor()
     */
    @Override
    public ClassDescriptor getClassDescriptor() {
        return DescriptorFactory.createClassDescriptorFromResourceName(getResourceName());
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
        ZipFileCodeBaseEntry other = (ZipFileCodeBaseEntry) obj;
        return this.codeBase.equals(other.codeBase) && this.zipEntry.equals(other.zipEntry);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 7919 * codeBase.hashCode() + zipEntry.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getCodeBase() + ":" + getResourceName();
    }
}
