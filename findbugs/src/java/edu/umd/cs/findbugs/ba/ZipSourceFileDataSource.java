/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A source file data source for source files residing in Zip or Jar
 * archives.
 */
public class ZipSourceFileDataSource implements SourceFileDataSource {
	private ZipFile zipFile;
	private ZipEntry zipEntry;

	public ZipSourceFileDataSource(ZipFile zipFile, ZipEntry zipEntry) {
		this.zipFile = zipFile;
		this.zipEntry = zipEntry;
	}

	public InputStream open() throws IOException {
		return zipFile.getInputStream(zipEntry);
	}

	public String getFullFileName() {
		return zipEntry.getName();
	}
}

// vim:ts=4
