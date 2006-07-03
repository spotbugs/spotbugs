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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of ICodeBase to read from a zip file or jar file.
 * 
 * @author David Hovemeyer
 */
public class ZipFileCodeBase extends AbstractScannableCodeBase {
	private ZipFile zipFile;

	/**
	 * Constructor.
	 * 
	 * @param fileName filename of the zip file
	 * @throws IOException
	 */
	public ZipFileCodeBase(String fileName) throws IOException {
		this.zipFile = new ZipFile(fileName);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param file the File representing the zip file
	 */
	public ZipFileCodeBase(File file) throws IOException {
		this.zipFile = new ZipFile(file);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#openResource(java.lang.String)
	 */
	public InputStream openResource(String resourceName) throws ResourceNotFoundException, IOException {
		ZipEntry entry = zipFile.getEntry(resourceName);
		if (entry == null) {
			throw new ResourceNotFoundException(resourceName);
		}
		return zipFile.getInputStream(entry);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#resourceNameIterator()
	 */
	public Iterator<String> resourceNameIterator() {
		final Enumeration<? extends ZipEntry> zipEntryEnumerator = zipFile.entries();
		
		return new Iterator<String>() {
			ZipEntry nextEntry;
			
			/* (non-Javadoc)
			 * @see java.util.Iterator#hasNext()
			 */
			public boolean hasNext() {
				scanForNextEntry();
				return nextEntry != null;
			}
			
			/* (non-Javadoc)
			 * @see java.util.Iterator#next()
			 */
			public String next() {
				scanForNextEntry();
				if (nextEntry == null) {
					throw new NoSuchElementException();
				}
				String result = nextEntry.getName();
				nextEntry = null;
				return result;
			}
			
			/* (non-Javadoc)
			 * @see java.util.Iterator#remove()
			 */
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void scanForNextEntry() {
				while (nextEntry == null) {
					if (!zipEntryEnumerator.hasMoreElements()) {
						return;
					}

					nextEntry = zipEntryEnumerator.nextElement();
				
					if (!nextEntry.isDirectory()) {
						break;
					}
				}
			}
			
		};
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
}
