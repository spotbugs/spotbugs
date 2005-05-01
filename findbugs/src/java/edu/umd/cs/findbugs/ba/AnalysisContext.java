/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

/*
// Not yet
import edu.umd.cs.findbugs.ba.type.BCELRepositoryClassResolver;
import edu.umd.cs.findbugs.ba.type.TypeRepository;
*/

import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.ch.Subtypes;


/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 *
 * @author David Hovemeyer
 */
public class AnalysisContext implements AnalysisFeatures {
	private static final boolean DEBUG_HIERARCHY = Boolean.getBoolean("findbugs.debug.hierarchy");
	
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private ClassContextCache classContextCache;
	private Subtypes subtypes;
	public Map analysisLocals = 
		Collections.synchronizedMap(new HashMap());
	private BitSet boolPropertySet;

    /*
      // JSR14 does not support Generic ThreadLocal
	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>();
    */
	private static InheritableThreadLocal currentAnalysisContext
		= new InheritableThreadLocal();
/*
	// Not yet
	private TypeRepository typeRepository;
*/

	/**
	 * The maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats
	 * about accesses.
	 */
	private static final int MAX_SIZE = CONSERVE_SPACE ? 1 : 60;

	private static class ClassContextCache extends LinkedHashMap<JavaClass, ClassContext> {
		private static final long serialVersionUID = 3258410621153196086L;

		public boolean removeEldestEntry(Map.Entry<JavaClass, ClassContext> entry) {
			return size() >= MAX_SIZE;
		}
	}

	/**
	 * Constructor.
	 */
	public AnalysisContext(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.sourceFinder = new SourceFinder();
		this.classContextCache = new ClassContextCache();
		this.subtypes = new Subtypes();
		this.boolPropertySet = new BitSet();
/*
		// Not yet
		// FIXME: eventually change to not use BCEL global repository
		this.typeRepository = new TypeRepository(new BCELRepositoryClassResolver());
*/

		currentAnalysisContext.set(this);
	}

	/**
	 * Get the AnalysisContext associated with this thread
	 */
	static public AnalysisContext currentAnalysisContext() {
		return (AnalysisContext)currentAnalysisContext.get();
	}

	/**
	 * Get the lookup failure callback.
	 */
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
	}

	/**
	 * Set the source path.
	 */
	public void setSourcePath(List<String> sourcePath) {
		sourceFinder.setSourceBaseList(sourcePath);
	}

	/**
	 * Get the SourceFinder, for finding source files.
	 */
	public SourceFinder getSourceFinder() {
		return sourceFinder;
	}

	public Subtypes getSubtypes() {
		return subtypes;
	}

	
	/**
	 * Clear the BCEL Repository in preparation for analysis.
	 */
	public void clearRepository() {
		// If the old repository backing store is a URLClassPathRepository
		// (which it certainly should be), destroy it.
		// This will close all underlying resources (archive files, etc.)
		org.apache.bcel.util.Repository repos = Repository.getRepository();
		if (repos instanceof URLClassPathRepository) {
			((URLClassPathRepository) repos).destroy();
		}
		
		// Purge repository of previous contents
		Repository.clearCache();

		// Clear InnerClassAccessMap cache.
		InnerClassAccessMap.instance().clearCache();

		// Create a URLClassPathRepository and make it current.
		URLClassPathRepository repository = new URLClassPathRepository(); 
		Repository.setRepository(repository);
	}
	
	/**
	 * Add an entry to the Repository's classpath.
	 * 
	 * @param url the classpath entry URL
	 * @throws IOException
	 */
	public void addClasspathEntry(String url) throws IOException {
		URLClassPathRepository repos = (URLClassPathRepository) Repository.getRepository();
		repos.addURL(url);
	}
	
	public void addApplicationClassToRepository(JavaClass appClass) {
		Repository.addClass(appClass);
		subtypes.addApplicationClass(appClass);
	}
		

	/**
	 * Get the ClassContext for a class.
	 *
	 * @param javaClass the class
	 * @return the ClassContext for that class
	 */
	public ClassContext getClassContext(JavaClass javaClass) {
		ClassContext classContext = classContextCache.get(javaClass);
		if (classContext == null) {
			classContext = new ClassContext(javaClass, this);
			classContextCache.put(javaClass, classContext);
		}
		return classContext;
	}
	
	/**
	 * Set a boolean property.
	 * 
	 * @param prop  the property to set
	 * @param value the value of the property
	 */
	public void setBoolProperty(int prop, boolean value) {
		boolPropertySet.set(prop, value);
	}
	
	/**
	 * Get a boolean property.
	 * 
	 * @param prop the property
	 * @return value of the property; defaults to false if the property
	 *         has not had a value assigned explicitly
	 */
	public boolean getBoolProperty(int prop) {
		return boolPropertySet.get(prop);
	}
}

// vim:ts=4
