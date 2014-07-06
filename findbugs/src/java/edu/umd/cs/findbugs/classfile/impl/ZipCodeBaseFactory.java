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

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;

import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * @author pugh
 */
public class ZipCodeBaseFactory {

    public static AbstractScannableCodeBase makeZipCodeBase(ICodeBaseLocator codeBaseLocator, File file) throws IOException {
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(ZipCodeBaseFactory.class);
        try {
            return new ZipFileCodeBase(codeBaseLocator, file);
        } catch (ZipException e) {
            // May be too many zip entries
            return new ZipInputStreamCodeBase(codeBaseLocator, file);
        } finally {
            profiler.end(ZipCodeBaseFactory.class);
        }
    }




}
