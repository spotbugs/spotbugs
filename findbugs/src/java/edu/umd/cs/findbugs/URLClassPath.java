/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.InputStream;
import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import java.util.zip.ZipFile;

/**
 * A work-alike class to use instead of BCEL's ClassPath class.
 * The main difference is that URLClassPath can load
 * classfiles from URLs.
 */
public class URLClassPath {
	private interface Entry {
		public InputStream openStream(String resourceName) throws IOException;
	}

	private static class ZipFileEntry {
		private ZipFile zipFile;
	}

	private List<Entry> entryList;

	public URLClassPath() {
		this.entryList = new LinkedList<Entry>();
	}
}

// vim:ts=4
