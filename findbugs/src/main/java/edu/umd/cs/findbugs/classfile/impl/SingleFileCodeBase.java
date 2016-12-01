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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.engine.ClassParser;
import edu.umd.cs.findbugs.classfile.engine.ClassParserInterface;
import edu.umd.cs.findbugs.io.IO;

/**
 * Implementation of ICodeBase for a single classfile.
 *
 * @author David Hovemeyer
 */
public class SingleFileCodeBase implements IScannableCodeBase {

    private final ICodeBaseLocator codeBaseLocator;

    private final String fileName;

    private boolean isAppCodeBase;

    private ICodeBase.Discovered howDiscovered;

    private long lastModifiedTime;

    private boolean resourceNameKnown;

    private String resourceName;

    public SingleFileCodeBase(ICodeBaseLocator codeBaseLocator, String fileName) {
        this.codeBaseLocator = codeBaseLocator;
        this.fileName = fileName;
        this.lastModifiedTime = new File(fileName).lastModified();
    }

    @Override
    public String toString() {
        return fileName;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getCodeBaseLocator()
     */
    @Override
    public ICodeBaseLocator getCodeBaseLocator() {
        return codeBaseLocator;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IScannableCodeBase#containsSourceFiles()
     */
    @Override
    public boolean containsSourceFiles() throws InterruptedException {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#iterator()
     */
    @Override
    public ICodeBaseIterator iterator() throws InterruptedException {
        return new ICodeBaseIterator() {
            boolean done = false;

            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#hasNext()
             */
            @Override
            public boolean hasNext() throws InterruptedException {
                return !done;
            }

            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
             */
            @Override
            public ICodeBaseEntry next() throws InterruptedException {
                if (done) {
                    throw new NoSuchElementException();
                }
                done = true;
                return new SingleFileCodeBaseEntry(SingleFileCodeBase.this);
            }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
     */
    @Override
    public ICodeBaseEntry lookupResource(String resourceName) {
        if (!resourceName.equals(getResourceName())) {
            return null;
        }

        return new SingleFileCodeBaseEntry(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.ICodeBase#setApplicationCodeBase(boolean)
     */
    @Override
    public void setApplicationCodeBase(boolean isAppCodeBase) {
        this.isAppCodeBase = isAppCodeBase;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#isApplicationCodeBase()
     */
    @Override
    public boolean isApplicationCodeBase() {
        return isAppCodeBase;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#setHowDiscovered(int)
     */
    @Override
    public void setHowDiscovered(ICodeBase.Discovered howDiscovered) {
        this.howDiscovered = howDiscovered;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getHowDiscovered()
     */
    @Override
    public ICodeBase.Discovered getHowDiscovered() {
        return howDiscovered;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#setLastModifiedTime(long)
     */
    @Override
    public void setLastModifiedTime(long lastModifiedTime) {
        if (lastModifiedTime > 0 && FindBugs.validTimestamp(lastModifiedTime)) {
            this.lastModifiedTime = lastModifiedTime;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getLastModifiedTime()
     */
    @Override
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getPathName()
     */
    @Override
    public String getPathName() {
        return fileName;
    }

    InputStream openFile() throws IOException {
        return new BufferedInputStream(new FileInputStream(fileName));
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
     */
    @Override
    public void close() {
        // Nothing to do
    }

    /**
     * Get the resource name of the single file. We have to open the file and
     * parse the constant pool in order to find this out.
     *
     * @return the resource name (e.g., "java/lang/String.class" if the class is
     *         java.lang.String)
     */
    String getResourceName() {
        if (!resourceNameKnown) {
            // The resource name of a classfile can only be determined by
            // reading
            // the file and parsing the constant pool.
            // If we can't do this for some reason, then we just
            // make the resource name equal to the filename.

            try {
                resourceName = getClassDescriptor().toResourceName();
            } catch (Exception e) {
                resourceName = fileName;
            }

            resourceNameKnown = true;
        }
        return resourceName;
    }

    ClassDescriptor getClassDescriptor() throws ResourceNotFoundException, InvalidClassFileFormatException {
        DataInputStream in = null;
        try {
            try {
                in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
                ClassParserInterface classParser = new ClassParser(in, null, new SingleFileCodeBaseEntry(this));
                ClassNameAndSuperclassInfo.Builder builder = new ClassNameAndSuperclassInfo.Builder();

                classParser.parse(builder);

                return builder.build().getClassDescriptor();
            } finally {
                if (in != null) {
                    IO.close(in);
                }
            }
        } catch (IOException e) {
            // XXX: file name isn't really the resource name, but whatever
            throw new ResourceNotFoundException(fileName);
        }
    }

    /**
     * Return the number of bytes in the file.
     *
     * @return the number of bytes in the file, or -1 if the file's length can't
     *         be determined
     */
    int getNumBytes() {
        File file = new File(fileName);
        // this is not needed but causes slowdown on a slow file system IO
        // file.length() returns zero if not found, and matches the contract of
        // this method
        // if (!file.exists()) {
        // return -1;
        // }
        return (int) file.length();
    }
}
