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

/*
 * Created on Sep 20, 2004
 */
package edu.umd.cs.findbugs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;

/**
 * BCEL Repository implementation that uses an URLClassPath
 * to find classes.  This class has two specific improvements
 * over BCEL's SyntheticRepository class:
 * <ol>
 * <li> Classpath elements may be added at any time, not
 *      just when the object is created.
 * <li> Classpath elements can be URLs.  This allows repository
 *      lookups to find classes via http URLs, jar URLs, etc.
 * </ol>
 * FindBugs requires and uses both of these capabilities.
 * 
 * @author David Hovemeyer
 */
public class URLClassPathRepository implements Repository {
	private Map<String, JavaClass> nameToClassMap;
	private URLClassPath urlClassPath;
	
	public URLClassPathRepository() {
		this.nameToClassMap = new HashMap<String, JavaClass>();
		this.urlClassPath = new URLClassPath();
	}

	/**
	 * Add a filename or URL to the classpath.
	 * @param fileName filename or URL of classpath entry to add
	 * @throws IOException
	 */
	public void addURL(String fileName) throws IOException {
		urlClassPath.addURL(fileName);
	}

	/**
	 * Add components of system classpath.
	 * @throws IOException
	 */
	public void addSystemClasspathComponents() throws IOException {
		String systemClassPath = ClassPath.getClassPath();
		StringTokenizer tok = new StringTokenizer(systemClassPath, File.pathSeparator);
		while (tok.hasMoreTokens()) {
			String entryName = tok.nextToken();
			try {
				urlClassPath.addURL(entryName);
			}
			catch (IOException e) {
				System.err.println("Warning: couldn't add path to classpath: " + entryName);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#storeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void storeClass(JavaClass javaClass) {
		nameToClassMap.put(javaClass.getClassName(), javaClass);
		javaClass.setRepository(this);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#removeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void removeClass(JavaClass javaClass) {
		nameToClassMap.remove(javaClass.getClassName());
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#findClass(java.lang.String)
	 */
	public JavaClass findClass(String className) {
		return nameToClassMap.get(className);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#loadClass(java.lang.String)
	 */
	public JavaClass loadClass(String className) throws ClassNotFoundException {
		//if (className.indexOf('/') >= 0) throw new IllegalStateException();
		JavaClass javaClass = findClass(className);
		if (javaClass == null) {
			javaClass = urlClassPath.lookupClass(className);
			storeClass(javaClass);
		}
		return javaClass;
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#loadClass(java.lang.Class)
	 */
	public JavaClass loadClass(Class clazz) throws ClassNotFoundException {
		return loadClass(clazz.getName());
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#clear()
	 */
	public void clear() {
		nameToClassMap.clear();
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#getClassPath()
	 */
	public ClassPath getClassPath() {
		return new ClassPath(urlClassPath.getClassPath());
	}
}

// vim:ts=4
