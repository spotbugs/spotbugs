/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;

/**
 * @author pugh
 */
public class ZipCodeBaseFactory {

	public static AbstractScannableCodeBase makeZipCodeBase(ICodeBaseLocator codeBaseLocator, File file) throws IOException {
		long size = file.length();
		long estimatedEntries = size / 2000;
		if (estimatedEntries < 20000)
			return new ZipFileCodeBase(codeBaseLocator, file);
		int zipEntries = 0;
		ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
		for(ZipEntry e; (e = in.getNextEntry()) != null; ) 
			zipEntries++;
		if (zipEntries < 30000)
			return new ZipFileCodeBase(codeBaseLocator, file);
		return new ZipInputStreamCodeBase(codeBaseLocator, file);
	}
		
}
