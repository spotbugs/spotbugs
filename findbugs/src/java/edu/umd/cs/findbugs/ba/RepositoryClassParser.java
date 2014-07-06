/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.io.IOException;
import java.io.InputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

/**
 * A special version of ClassParser that automatically enters parsed classes
 * into the Repository. This allows us to use the Repository to inspect the
 * class hierarchy, based on the current class path.
 */
public class RepositoryClassParser {
    private final ClassParser classParser;

    /**
     * Constructor.
     *
     * @param inputStream
     *            the input stream from which to read the class file
     * @param fileName
     *            filename of the class file
     */
    public RepositoryClassParser(InputStream inputStream, String fileName) {
        classParser = new ClassParser(inputStream, fileName);
    }

    /**
     * Constructor.
     *
     * @param fileName
     *            name of the class file
     */
    public RepositoryClassParser(String fileName)  {
        classParser = new ClassParser(fileName);
    }

    /**
     * Constructor.
     *
     * @param zipFile
     *            name of a zip file containing the class
     * @param fileName
     *            name of the zip entry within the class
     */
    public RepositoryClassParser(String zipFile, String fileName) {
        classParser = new ClassParser(zipFile, fileName);
    }

    /**
     * Parse the class file into a JavaClass object. If succesful, the new
     * JavaClass is entered into the Repository.
     *
     * @return the parsed JavaClass
     * @throws IOException
     *             if the class cannot be parsed
     */
    public JavaClass parse() throws IOException {
        JavaClass jclass = classParser.parse();
        Repository.addClass(jclass);
        return jclass;
    }
}

