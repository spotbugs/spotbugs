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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Implementation of ICodeBase to read from a zip file or jar file.
 *
 * @author David Hovemeyer
 */
public class ZipInputStreamCodeBase extends AbstractScannableCodeBase {
	final static boolean DEBUG = false;

	final File file;

	final MapCache<String, ZipInputStreamCodeBaseEntry> map = new MapCache<String, ZipInputStreamCodeBaseEntry>(10000);

	final HashSet<String> entries = new HashSet<String>();

	/**
	 * Constructor.
	 *
	 * @param codeBaseLocator the codebase locator for this codebase
	 * @param file the File containing the zip file (may be a temp file
	 *         if the codebase was copied from a nested zipfile in
	 *         another codebase)
	 */
	public ZipInputStreamCodeBase(ICodeBaseLocator codeBaseLocator, File file) throws IOException {
		super(codeBaseLocator);

		this.file = file;
		setLastModifiedTime(file.lastModified());
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		ZipEntry ze;
		if (DEBUG) {
	        System.out.println("Reading zip input stream " + file);
        }
		int count = 0;
		while ((ze = zis.getNextEntry()) != null) {
			if (!ze.isDirectory() && (ze.getName().equals("META-INF/MANIFEST.MF") || ze.getName().endsWith(".class"))) {
				entries.add(ze.getName());
				if (ze.getName().equals("META-INF/MANIFEST.MF")) {
	                map.put(ze.getName(), build(zis, ze));
                }
			}
			zis.closeEntry();

		}
		if (DEBUG) {
	        System.out.println("Done with zip input stream " + file);
        }

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#lookupResource(java.lang.String)
	 */
	public ICodeBaseEntry lookupResource(String resourceName) {

		// Translate resource name, in case a resource name
		// has been overridden and the resource is being accessed
		// using the overridden name.
		resourceName = translateResourceName(resourceName);
		if (!entries.contains(resourceName)) {
	        return null;
        }

		try {
			ZipInputStreamCodeBaseEntry z = map.get(resourceName);
			if (z != null) {
	            return z;
            }
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			ZipEntry ze;
			boolean found = false;
			int countDown = 20;
			while ((ze = zis.getNextEntry()) != null && countDown >= 0) {
				if (ze.getName().equals(resourceName)) {
	                found = true;
                }
				if (found) {
					countDown--;
					if (map.containsKey(ze.getName())) {
	                    continue;
                    }
					z = build(zis, ze);
					map.put(ze.getName(), z);
				}
				zis.closeEntry();
			}
			z = map.get(resourceName);
			if (z == null) {
	            throw new AssertionError("Could not find " + resourceName);
            }
			return z;
		} catch (IOException e) {
			return null;
		}
	}

	ZipInputStreamCodeBaseEntry build(ZipInputStream zis, ZipEntry ze) throws IOException {
		long sz = ze.getSize();
		ByteArrayOutputStream out;
		if (sz < 0 || sz > Integer.MAX_VALUE) {
	        out = new ByteArrayOutputStream();
        } else {
	        out = new ByteArrayOutputStream((int) sz);
        }
		IO.copy(zis, out);
		byte[] bytes = out.toByteArray();
		setLastModifiedTime(ze.getTime());
		return new ZipInputStreamCodeBaseEntry(this, ze, bytes);
	}

	class MyIterator implements ICodeBaseIterator {
		ZipInputStream zis;

		ZipEntry ze;

		MyIterator() {
			try {
				zis = new ZipInputStream(new FileInputStream(file));
				ze = zis.getNextEntry();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void getNextEntry() throws IOException {
			ze = zis.getNextEntry();
			if (ze == null) {
	            zis.close();
            }
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#hasNext()
		 */
		public boolean hasNext() throws InterruptedException {
			if (Thread.interrupted()) {
	            throw new InterruptedException();
            }

			return ze != null;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
		 */
		public ICodeBaseEntry next() throws InterruptedException {
			try {
				if (Thread.interrupted()) {
	                throw new InterruptedException();
                }
				ZipInputStreamCodeBaseEntry z = build(zis, ze);
				zis.closeEntry();
				getNextEntry();
				return z;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public ICodeBaseIterator iterator() {
		return new MyIterator();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#getPathName()
	 */
	public String getPathName() {
		return file.getName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return file.getName();
	}
}
