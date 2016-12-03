/**
 * Find Security Bugs
 * Copyright (c) Philippe Arteau, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package edu.umd.cs.findbugs.test.service;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.net.URL;

public class ClassFileLocator {

    /**
     * @param path
     * @return Full path to the class file base on class name.
     */
    public String getClassFilePath(String path) {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource(path + ".class");
        if(url != null) {
            return getFilenameFromUrl(url);
        }
        url = cl.getResource(path);
        assertNotNull("No class found for the path = " + path, url);
        return getFilenameFromUrl(url);
    }

    public String getJspFilePath(String path) {
        ClassLoader cl = getClass().getClassLoader();

        //This is subject to change base on the JSP compiler implementation
        String generatedClassName = path.replaceAll("_","_005f").replace(".jsp","_jsp");
        URL url = cl.getResource("jsp/"+generatedClassName+".class");
        if(url == null) {
            url = cl.getResource("org/apache/jsp/"+generatedClassName+".class");
        }

        assertNotNull("No jsp file found for the path = " + path, url);
        return getFilenameFromUrl(url);
    }

    public String getJarFilePath(String path) {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource(path);
        assertNotNull("No jar found for the path = " + path, url);
        return getFilenameFromUrl(url);
    }

    private String getFilenameFromUrl(URL url) {
        String filename;
        try {
            filename = url.toURI().getPath();
        } catch (final URISyntaxException e) {
            fail("Failed to get file path = " + url);
            return null;
        }

        final String prefix = "file:";
        if (filename.startsWith(prefix)) {
            filename = filename.substring(prefix.length());
        }
        return filename;
    }
}
