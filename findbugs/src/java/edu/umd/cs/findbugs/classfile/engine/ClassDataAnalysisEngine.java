/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.RecomputableClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.impl.ZipInputStreamCodeBaseEntry;
import edu.umd.cs.findbugs.io.IO;

/**
 * Analysis engine to produce the data (bytes) of a class.
 * 
 * @author David Hovemeyer
 */
public class ClassDataAnalysisEngine extends RecomputableClassAnalysisEngine<ClassData> {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public ClassData analyze(IAnalysisCache analysisCache,
			ClassDescriptor descriptor) throws CheckedAnalysisException {

		// Compute the resource name
		String resourceName = descriptor.toResourceName();

		// Look up the codebase entry for the resource
		ICodeBaseEntry codeBaseEntry;
		try {
			codeBaseEntry = analysisCache.getClassPath().lookupResource(resourceName);
		} catch (ResourceNotFoundException e) {
			throw new MissingClassException(descriptor, e);
		}

		byte[] data;
		if (codeBaseEntry instanceof ZipInputStreamCodeBaseEntry) {
			data = ((ZipInputStreamCodeBaseEntry)codeBaseEntry).getBytes();
		} else {
			// Create a ByteArrayOutputStream to capture the class data
			int length = codeBaseEntry.getNumBytes();
			ByteArrayOutputStream byteSink;
			if (length >= 0) {
				byteSink = new ByteArrayOutputStream(length);
			} else {
				byteSink = new ByteArrayOutputStream();
			}

			// Read the class data into the byte array
			InputStream in = null;
			try {
				in = codeBaseEntry.openResource();
				IO.copy(in, byteSink);
			} catch (IOException e) {
				throw new MissingClassException(descriptor, e);
			} finally {
				if (in != null) {
					IO.close(in);
				}
			}

			// Construct the resulting ClassData object and return it
			data = byteSink.toByteArray();
		}
		return new ClassData(descriptor, codeBaseEntry, data);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(ClassData.class, this);
	}

	
}
