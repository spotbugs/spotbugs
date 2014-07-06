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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.io.IO;

/**
 * A scannable code base class for a zip (or Jar) file nested inside some other
 * codebase. These are handled by extracting the nested zip/jar file to a
 * temporary file, and delegating to an internal ZipFileCodeBase that reads from
 * the temporary file.
 *
 * @author David Hovemeyer
 */
public class NestedZipFileCodeBase extends AbstractScannableCodeBase {
    private final ICodeBase parentCodeBase;

    private final String resourceName;

    private File tempFile;

    private AbstractScannableCodeBase delegateCodeBase;

    /**
     * Constructor.
     *
     * @param codeBaseLocator
     *            the codebase locator for this codebase
     */
    public NestedZipFileCodeBase(NestedZipFileCodeBaseLocator codeBaseLocator) throws ResourceNotFoundException, IOException {
        super(codeBaseLocator);
        this.parentCodeBase = codeBaseLocator.getParentCodeBase();
        this.resourceName = codeBaseLocator.getResourceName();

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Create a temp file
            this.tempFile = File.createTempFile("findbugs", ".zip");
            tempFile.deleteOnExit(); // just in case we crash before the
            // codebase is closed

            // Copy nested zipfile to the temporary file
            // FIXME: potentially long blocking operation - should be
            // interruptible
            ICodeBaseEntry resource = parentCodeBase.lookupResource(resourceName);
            if (resource == null) {
                throw new ResourceNotFoundException(resourceName);
            }
            inputStream = resource.openResource();
            outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            IO.copy(inputStream, outputStream);
            outputStream.flush();

            // Create the delegate to read from the temporary file
            delegateCodeBase = ZipCodeBaseFactory.makeZipCodeBase(codeBaseLocator, tempFile);
        } finally {
            if (inputStream != null) {
                IO.close(inputStream);
            }

            if (outputStream != null) {
                IO.close(outputStream);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IScannableCodeBase#iterator()
     */
    @Override
    public ICodeBaseIterator iterator() throws InterruptedException {
        return new DelegatingCodeBaseIterator(this, delegateCodeBase);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
     */
    @Override
    public ICodeBaseEntry lookupResource(String resourceName) {
        ICodeBaseEntry delegateCodeBaseEntry = delegateCodeBase.lookupResource(resourceName);
        if (delegateCodeBaseEntry == null) {
            return null;
        }
        return new DelegatingCodeBaseEntry(this, delegateCodeBaseEntry);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#getPathName()
     */
    @Override
    public String getPathName() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
     */
    @Override
    public void close() {
        delegateCodeBase.close();
        if (!tempFile.delete()) {
            AnalysisContext.logError("Could not delete " + tempFile);
        }
    }
}
