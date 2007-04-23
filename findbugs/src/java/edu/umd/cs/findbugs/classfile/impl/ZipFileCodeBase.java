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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of ICodeBase to read from a zip file or jar file.
 * 
 * @author David Hovemeyer
 */
public class ZipFileCodeBase extends AbstractScannableCodeBase {
	ZipFile zipFile;

	/**
	 * Constructor.
	 * 
	 * @param codeBaseLocator the codebase locator for this codebase
	 * @param file the File containing the zip file (may be a temp file
	 *         if the codebase was copied from a nested zipfile in
	 *         another codebase)
	 */
	public ZipFileCodeBase(ICodeBaseLocator codeBaseLocator, File file) throws IOException {
		super(codeBaseLocator);
		try {
		this.zipFile = new ZipFile(file);
		setLastModifiedTime(file.lastModified());
		} catch (ZipException e) {
			throw new ZipException("Error opening " + file);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) throws ResourceNotFoundException {
		// Translate resource name, in case a resource name
		// has been overridden and the resource is being accessed
		// using the overridden name.
		resourceName = translateResourceName(resourceName);

	try {
		ZipEntry entry = zipFile.getEntry(resourceName);
		if (entry == null) {
			throw new ResourceNotFoundException(resourceName);
		}
		return new ZipFileCodeBaseEntry(this, entry);
	}
	catch (IllegalStateException ise) {
		// zipFile.getEntry() throws IllegalStateException if the zip file has been closed
		throw new ResourceNotFoundException(resourceName, ise);
	}
	}

	public ICodeBaseIterator iterator() {
		final Enumeration<? extends ZipEntry> zipEntryEnumerator = zipFile.entries();

		return new ICodeBaseIterator() {
			ZipFileCodeBaseEntry nextEntry;

			public boolean hasNext() {
				scanForNextEntry();
				return nextEntry != null;
			}

			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
			 */
			public ICodeBaseEntry next() throws InterruptedException {
				scanForNextEntry();
				if (nextEntry == null) {
					throw new NoSuchElementException();
				}
				ICodeBaseEntry result = nextEntry;
				nextEntry = null;
				return result;
			}

			private void scanForNextEntry() {
				while (nextEntry == null) {
					if (!zipEntryEnumerator.hasMoreElements()) {
						return;
					}

					ZipEntry zipEntry = zipEntryEnumerator.nextElement();

					if (!zipEntry.isDirectory()) {
						setLastModifiedTime(zipEntry.getTime());
						nextEntry = new ZipFileCodeBaseEntry(ZipFileCodeBase.this, zipEntry);
						break;
					}
				}
			}

		};
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getPathName()
	 */
	public String getPathName() {
		return zipFile.getName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		try {
			zipFile.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return zipFile.getName();
	}
}
