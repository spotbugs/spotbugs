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

import edu.umd.cs.findbugs.FindBugs2;
import java.io.File;
import java.io.IOException;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Factory to create codebase/classpath/classfile objects.
 *
 * @author David Hovemeyer
 */
public class ClassFactory implements IClassFactory {
    private static IClassFactory theInstance = new ClassFactory();

    private ClassFactory() {
    }

    public static IClassFactory instance() {
        return theInstance;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.impl.IClassFactory#createClassPath()
     */
    @Override
    public IClassPath createClassPath() {
        return new ClassPathImpl();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassFactory#createClassPathBuilder(edu
     * .umd.cs.findbugs.classfile.IErrorLogger)
     */
    @Override
    public IClassPathBuilder createClassPathBuilder(IErrorLogger errorLogger) {
        return new ClassPathBuilder(this, errorLogger);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.impl.IClassFactory#
     * createFilesystemCodeBaseLocator(java.lang.String)
     */
    @Override
    public ICodeBaseLocator createFilesystemCodeBaseLocator(String pathName) {
        return new FilesystemCodeBaseLocator(pathName);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IClassFactory#
     * createNestedArchiveCodeBaseLocator
     * (edu.umd.cs.findbugs.classfile.ICodeBase, java.lang.String)
     */
    @Override
    public ICodeBaseLocator createNestedArchiveCodeBaseLocator(ICodeBase parentCodeBase, String path) {
        return new NestedZipFileCodeBaseLocator(parentCodeBase, path);
    }

    static IScannableCodeBase createFilesystemCodeBase(FilesystemCodeBaseLocator codeBaseLocator) throws IOException {
        String fileName = codeBaseLocator.getPathName();

        File file = new File(fileName);

        if (!file.exists() || !file.canRead()) {
            return createEmptyCodeBase(codeBaseLocator, file);
        } else if (file.isDirectory()) {
            return new DirectoryCodeBase(codeBaseLocator, file);
        } else if (!file.isFile()) {
            return createEmptyCodeBase(codeBaseLocator, file);
        } else if (fileName.endsWith(".class")) {
            return new SingleFileCodeBase(codeBaseLocator, fileName);
        } else if (fileName.endsWith(File.separator + "jrt-fs.jar")) {
            return new JrtfsCodeBase(codeBaseLocator, fileName);
        } else {
            try {
                return ZipCodeBaseFactory.makeZipCodeBase(codeBaseLocator, file);
            } catch (IOException e) {
                return createEmptyCodeBase(codeBaseLocator, file);
            }
        }
    }

    private static IScannableCodeBase createEmptyCodeBase(FilesystemCodeBaseLocator codeBaseLocator, File file) {
        if (FindBugs2.DEBUG) {
            System.out.println("Ignoring unreadable or non-existent file " + file);
        }
        return new EmptyCodeBase(codeBaseLocator);
    }

    static IScannableCodeBase createNestedZipFileCodeBase(NestedZipFileCodeBaseLocator codeBaseLocator)
            throws ResourceNotFoundException, IOException {
        return new NestedZipFileCodeBase(codeBaseLocator);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassFactory#createAnalysisCache(edu.umd
     * .cs.findbugs.classfile.IClassPath)
     */
    @Override
    public IAnalysisCache createAnalysisCache(IClassPath classPath, BugReporter errorLogger) {
        return new AnalysisCache(classPath, errorLogger);
    }
}
