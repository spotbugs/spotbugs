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

package edu.umd.cs.findbugs.ba.ch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Support for class hierarchy queries.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
@Deprecated // use Subtypes2 instead
public class Subtypes {
	private static final boolean DEBUG_HIERARCHY = false || SystemProperties.getBoolean("findbugs.debug.hierarchy");

	
	public Subtypes() {
	}

	/**
	 * Get immediate subtypes of given class or interface.
	 * 
	 * @param c a class or interface
	 * @return set of immediate subtypes
	 */

	public Set<JavaClass> getImmediateSubtypes(JavaClass c) {
		ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor(c);
		try {
	        return getJavaClasses(subtypes2().getSubtypes(classDescriptor));
        } catch (ClassNotFoundException e) {
	        AnalysisContext.reportMissingClass(e);
	        return Collections.emptySet();
        } catch (CheckedAnalysisException e) {
        	 AnalysisContext.logError("Error checking subtypes of " + c.getClassName(), e);
 	        return Collections.emptySet();
        }
	}
	private  JavaClass getJavaClass(ClassDescriptor descriptor) throws CheckedAnalysisException {
		return Global.getAnalysisCache().getClassAnalysis(JavaClass.class, descriptor);
	}
	private JavaClass getJavaClass(XClass xClass) throws CheckedAnalysisException {
		return Global.getAnalysisCache().getClassAnalysis(JavaClass.class, xClass.getClassDescriptor());
	}
		
	
	private Set<JavaClass> getJavaClasses(Collection<ClassDescriptor> descriptors) throws CheckedAnalysisException {
		HashSet<JavaClass> result = new HashSet<JavaClass>();
		for(ClassDescriptor c : descriptors) 
			result.add(getJavaClass(c));
		return result;
	}
	
	private Set<JavaClass> getJavaClassesFromXClasses(Collection<XClass> xclasses) throws CheckedAnalysisException {
		HashSet<JavaClass> result = new HashSet<JavaClass>();
		for(XClass c : xclasses) 
			result.add(getJavaClass(c));
		return result;
	}
	private static Subtypes2 subtypes2() {
		AnalysisContext analysisContext = AnalysisContext
		.currentAnalysisContext();
		return analysisContext.getSubtypes2();
	}
	/**
	 * Determine if a class or interface has subtypes
	 * 
	 * @param c a class or interface
	 * @return true if c has any subtypes/interfaces
	 */
	public boolean hasSubtypes(JavaClass c) {
		ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor(c);
		try {
	        return !subtypes2().getDirectSubtypes(classDescriptor).isEmpty();
        } catch (ClassNotFoundException e) {
	        AnalysisContext.reportMissingClass(e);
	        return false;
        }
	}

	/**
	 * Get set of all known classes and interfaces. 
	 * 
	 * @return set of all known classes and interfaces
	 */
	@Deprecated
	public Set<JavaClass> getAllClasses() {
		assert false;
		try {
	        return getJavaClassesFromXClasses(subtypes2().getXClassCollection());
        } catch (CheckedAnalysisException e) {
	        throw new AssertionError("We're screwed");
        }
	}

	/**
	 * Get set of all transitive subtypes of given class or interface,
	 * <em>not including the class or interface itself</em>.
	 * 
	 * @param c a class or interface
	 * @return set of all transitive subtypes
	 */
	public Set<JavaClass> getTransitiveSubtypes(JavaClass c) {
		assert !c.getClassName().equals("java.lang.Object");
		ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor(c);
		try {
	        return getJavaClasses(subtypes2().getSubtypes(classDescriptor));
        } catch (ClassNotFoundException e) {
	        AnalysisContext.reportMissingClass(e);
	        return Collections.emptySet();
        } catch (CheckedAnalysisException e) {
        	 AnalysisContext.logError("Error checking subtypes of " + c.getClassName(), e);
 	        return Collections.emptySet();
        }
	}

	/**
	 * Get set of all known transitive classes and interfaces which are subtypes of
	 * both of the given classes and/or interfaces.  Note that in this method,
	 * we consider a class to be a subtype of itself.  Therefore, this method
	 * can be used to determine, e.g., if there are any classes implementing
	 * both of two given interfaces.
	 * 
	 * @param a a class or interface
	 * @param b another class or interface
	 * @return set of all common subtypes of <i>a</i> and <i>b</i>
	 */
	public Set<JavaClass> getTransitiveCommonSubtypes(JavaClass a, JavaClass b) {
		ClassDescriptor aD = DescriptorFactory.createClassDescriptor(a);
		ClassDescriptor bD = DescriptorFactory.createClassDescriptor(b);
		try {
	        return getJavaClasses(subtypes2().getTransitiveCommonSubtypes(aD, bD));
        } catch (ClassNotFoundException e) {
	        AnalysisContext.reportMissingClass(e);
	        return Collections.emptySet();
        } catch (CheckedAnalysisException e) {
        	 AnalysisContext.logError("Error checking common subtypes of " + a.getClassName() + " and " + b.getClassName(), e);
 	        return Collections.emptySet();
        }
		
	}

	
	public static void learnFieldsAndMethods(JavaClass c) {
		for(Field f : c.getFields())
			XFactory.createXField(c, f);
		for(Method m : c.getMethods())
			XFactory.createXMethod(c, m);
	}
	@Deprecated
	public void addNamedClass(String name) {

	}
	@Deprecated
	public void addApplicationClass(JavaClass c) {

	}
	@Deprecated
	public void addClass(JavaClass c) {
		
	}
	
	public static String extractClassName(String originalName) {
		String name = originalName;
		if (name.charAt(0) != '[' && name.charAt(name.length() - 1) != ';')
			return name;
		while (name.charAt(0) == '[')
			name = name.substring(1);
		if (name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';')
			name = name.substring(1, name.length() - 1);
		if (name.charAt(0) == '[') throw new IllegalArgumentException("Bad class name: " + originalName);
		return name;
	}

	/**
	 * Determine whether or not the given class is an application class.
	 * 
	 * @param javaClass a class
	 * @return true if it's an application class, false if not
	 */
	public boolean isApplicationClass(JavaClass javaClass) {
		return subtypes2().isApplicationClass(DescriptorFactory.createClassDescriptor(javaClass));
	}
}
