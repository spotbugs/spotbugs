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

package edu.umd.cs.findbugs.classfile.analysis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

/**
 * The data (bytes) of a class.
 *
 * @author David Hovemeyer
 */
public class ClassData {
    private final ClassDescriptor classDescriptor;

    private final ICodeBaseEntry codeBaseEntry;

    private final byte[] data;

    /**
     * Constructor.
     *
     * @param classDescriptor
     *            descriptor for the class
     * @param data
     *            the data (bytes) for a class
     */
    public ClassData(ClassDescriptor classDescriptor, ICodeBaseEntry codeBaseEntry, byte[] data) {
        this.classDescriptor = classDescriptor;
        this.codeBaseEntry = codeBaseEntry;
        this.data = data;
    }

    /**
     * @return Returns the ClassDescriptor.
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    /**
     * @return Returns the codeBaseEntry.
     */
    public ICodeBaseEntry getCodeBaseEntry() {
        return codeBaseEntry;
    }

    /**
     * @return Returns the data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Open an InputStream on the class data.
     *
     * @return InputStream reading from the class data
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }


    @Override
    public String toString() {
        return codeBaseEntry + ":" + classDescriptor;
    }
}
