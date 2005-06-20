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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	public static final boolean DEBUG = Boolean.getBoolean("findbugs.classpath.debug");
	
	private static final long serialVersionUID = 1L;

	private Map<String, JavaClass> nameToClassMap;
	private URLClassPath urlClassPath;
	private Set<String> knownClasses;
	
	public URLClassPathRepository() {
		this.nameToClassMap = new HashMap<String, JavaClass>();
		this.urlClassPath = new URLClassPath();
		if (DEBUG) {
			this.knownClasses = new HashSet<String>();
		}
	}
	
	/**
	 * Clear the repository and close all underlying resources.
	 */
	public void destroy() {
		nameToClassMap.clear();
		urlClassPath.close();
		if (DEBUG) {
			System.out.println("Destroying Repository");
			knownClasses.clear();
			dumpStack();
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
		javaClass.setRepository(this);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#removeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void removeClass(JavaClass javaClass) {
		nameToClassMap.remove(javaClass.getClassName());
		if (DEBUG) {
			System.out.println("Removing class " + javaClass.getClassName() + " from Repository");
			dumpStack();
			knownClasses.remove(javaClass.getClassName());
		}
	}

	private void dumpStack() {
		new Throwable().printStackTrace(System.out);
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
			if (DEBUG) {
				if (knownClasses.contains(className)) {
					System.out.println("MASSIVE ERROR: " + className + " should be in the Repository already!");
				}
				System.out.println("Looking up " + className + " on classpath");
				dumpStack();
			}
			javaClass = urlClassPath.lookupClass(className);
			if (DEBUG) System.out.println("Storing " + className + " in repository");
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
		if (DEBUG) {
			knownClasses.clear();
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#getClassPath()
	 */
	public ClassPath getClassPath() {
		return new ClassPath(urlClassPath.getClassPath());
	}
}

// vim:ts=4
