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
package edu.umd.cs.findbugs.ba;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.NonNull;


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
	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.classpath.debug");
	
	private static final long serialVersionUID = 1L;

	private Map<String, JavaClass> nameToClassMap;
	private URLClassPath urlClassPath;
	
	public URLClassPathRepository() {
		this.nameToClassMap = new HashMap<String, JavaClass>();
		this.urlClassPath = new URLClassPath();
	}
	
	/**
	 * Clear the repository and close all underlying resources.
	 */
	public void destroy() {
		nameToClassMap.clear();
		urlClassPath.close();
		if (DEBUG) {
			System.out.println("Destroying Repository");
		}
	}

	/**
	 * Add a filename or URL to the classpath.
	 * @param fileName filename or URL of classpath entry to add
	 * @throws IOException
	 */
	public void addURL(String fileName) throws IOException {
		urlClassPath.addURL(fileName);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#storeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void storeClass(JavaClass javaClass) {
		if (DEBUG) System.out.println("Storing class " + javaClass.getClassName() + " in repository");
		JavaClass previous = nameToClassMap.put(javaClass.getClassName(), javaClass);
		if (DEBUG && previous != null) {
			System.out.println("\t==> A previous class was evicted!");
			dumpStack();
		}
		Repository tmp = org.apache.bcel.Repository.getRepository();
		if (tmp != null && tmp != this) 
			throw new IllegalStateException("Wrong/multiple BCEL repository");
		if (tmp == null)
			org.apache.bcel.Repository.setRepository(this);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#removeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void removeClass(JavaClass javaClass) {
		nameToClassMap.remove(javaClass.getClassName());
		if (DEBUG) {
			System.out.println("Removing class " + javaClass.getClassName() + " from Repository");
			dumpStack();
		}
	}

	private void dumpStack() {
		new Throwable().printStackTrace(System.out);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#findClass(java.lang.String)
	 */
	public JavaClass findClass(@NonNull String className) {
		// Make sure we handle class names with slashes.
		// If we don't, we can get into serious trouble: a previously
		// loaded class will appear to be missing (because we're using the
		// wrong name to look it up) and be evicted by some other random
		// version of the class loaded from the classpath.
		String dottedClassName = className.replace('/', '.');
		
		return nameToClassMap.get(dottedClassName);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#loadClass(java.lang.String)
	 */
	public JavaClass loadClass(@NonNull String className) throws ClassNotFoundException {
		if (className == null) throw new IllegalArgumentException("className is null");
		//if (className.indexOf('/') >= 0) throw new IllegalStateException();
		JavaClass javaClass = findClass(className);
		if (javaClass == null) {
			if (DEBUG) {
				System.out.println("Looking up " + className + " on classpath");
				dumpStack();
			}
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
		if (DEBUG) {
			System.out.println("Clearing Repository!");
			dumpStack();
		}
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
